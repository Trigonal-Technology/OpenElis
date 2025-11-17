package us.mn.state.health.lims.labbridge.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

import us.mn.state.health.lims.analysis.dao.AnalysisDAO;
import us.mn.state.health.lims.analysis.daoimpl.AnalysisDAOImpl;
import us.mn.state.health.lims.analysis.valueholder.Analysis;
import us.mn.state.health.lims.common.util.SystemConfiguration;
import us.mn.state.health.lims.dictionary.dao.DictionaryDAO;
import us.mn.state.health.lims.dictionary.daoimpl.DictionaryDAOImpl;
import us.mn.state.health.lims.dictionary.valueholder.Dictionary;
import us.mn.state.health.lims.result.dao.ResultDAO;
import us.mn.state.health.lims.result.daoimpl.ResultDAOImpl;
import us.mn.state.health.lims.result.daoimpl.ResultSignatureDAOImpl;
import us.mn.state.health.lims.result.valueholder.Result;
import us.mn.state.health.lims.result.valueholder.ResultSignature;
import us.mn.state.health.lims.sample.valueholder.Sample;
import us.mn.state.health.lims.sampleitem.valueholder.SampleItem;
import us.mn.state.health.lims.statusofsample.util.StatusOfSampleUtil;
import us.mn.state.health.lims.systemuser.daoimpl.SystemUserDAOImpl;
import us.mn.state.health.lims.systemuser.valueholder.SystemUser;
import us.mn.state.health.lims.test.dao.TestDAO;
import us.mn.state.health.lims.test.daoimpl.TestDAOImpl;
import us.mn.state.health.lims.test.valueholder.Test;
import org.bahmni.feed.openelis.feed.service.EventPublishers;
import org.bahmni.feed.openelis.feed.service.impl.OpenElisUrlPublisher;
import java.util.Arrays;
import us.mn.state.health.lims.testanalyte.dao.TestAnalyteDAO;
import us.mn.state.health.lims.testanalyte.daoimpl.TestAnalyteDAOImpl;
import us.mn.state.health.lims.testanalyte.valueholder.TestAnalyte;
import us.mn.state.health.lims.testresult.dao.TestResultDAO;
import us.mn.state.health.lims.testresult.daoimpl.TestResultDAOImpl;
import us.mn.state.health.lims.testresult.valueholder.TestResult;
import us.mn.state.health.lims.sample.dao.SampleDAO;
import us.mn.state.health.lims.sample.daoimpl.SampleDAOImpl;
import us.mn.state.health.lims.common.util.DateUtil;
import us.mn.state.health.lims.common.util.SystemConfiguration;
import us.mn.state.health.lims.statusofsample.util.StatusOfSampleUtil;
import us.mn.state.health.lims.resultlimits.valueholder.ResultLimit;
import us.mn.state.health.lims.result.action.util.ResultsLoadUtility;
import us.mn.state.health.lims.samplehuman.dao.SampleHumanDAO;
import us.mn.state.health.lims.samplehuman.daoimpl.SampleHumanDAOImpl;
import us.mn.state.health.lims.patient.valueholder.Patient;
import us.mn.state.health.lims.common.util.StringUtil;

/**
 * Service layer that wraps the existing UI logic for updating test results
 * This ensures REST endpoints follow the same business logic as the UI
 */
public class ResultUpdateService {

    public static class ResultUpdateResponse {
        public boolean success;
        public String message;
        public String analysisId;
        public String testId;
        public String testName;
        public String accessionNumber;
        public String resultValue;
        public String resultType;
        public boolean statusUpdated;
        public int rowsAffected;
        
        public ResultUpdateResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public ResultUpdateResponse updateTestResult(String analysisId, String testId, 
                                               String resultValue, String resultType, 
                                               String sysUserId) {
        try {
            // Validate input parameters
            if (analysisId == null || testId == null || resultValue == null) {
                return new ResultUpdateResponse(false, "Missing required parameters: analysisId, testId, resultValue");
            }

            // Get analysis and validate it exists and is in correct status
            AnalysisDAO analysisDAO = new AnalysisDAOImpl();
            Analysis analysis = new Analysis();
            analysis.setId(analysisId);
            analysisDAO.getData(analysis);
            
            if (analysis.getId() == null) {
                return new ResultUpdateResponse(false, "Analysis not found: " + analysisId);
            }

            // Validate test exists and matches analysis
            TestDAO testDAO = new TestDAOImpl();
            Test test = new Test();
            test.setId(testId);
            testDAO.getData(test);
            
            if (test.getId() == null) {
                return new ResultUpdateResponse(false, "Test not found: " + testId);
            }

            // Verify analysis is for the correct test
            if (!analysis.getTest().getId().equals(testId)) {
                return new ResultUpdateResponse(false, "Test ID mismatch for analysis");
            }

            // Get sample information
            SampleItem sampleItem = analysis.getSampleItem();
            if (sampleItem == null) {
                return new ResultUpdateResponse(false, "Sample item not found for analysis");
            }

            Sample sample = sampleItem.getSample();
            if (sample == null) {
                return new ResultUpdateResponse(false, "Sample not found for analysis");
            }

            // Resolve the appropriate TestResult and normalized value/type based on input
            TestResultDAO testResultDAO = new TestResultDAOImpl();
            ResolvedResult rr = resolveTestResultAndValue(test, testResultDAO, resultValue, resultType);
            if (!rr.success) {
                return new ResultUpdateResponse(false, rr.message);
            }
            TestResult testResult = rr.testResult;
            String normalizedValue = rr.value;
            String normalizedType = rr.type;

            // Get test analytes for this test
            TestAnalyteDAO testAnalyteDAO = new TestAnalyteDAOImpl();
            List<TestAnalyte> testAnalytes = testAnalyteDAO.getAllTestAnalytesPerTest(test);
            
            // Use the existing DAO pattern to update results
            ResultDAO resultDAO = new ResultDAOImpl();
            
            // Handle tests with or without analytes
            if (testAnalytes == null || testAnalytes.isEmpty()) {
                // For tests without analytes, create or update a direct result entry
                return updateDirectResult(analysis, test, testResult, normalizedValue, normalizedType, sysUserId, resultDAO);
            }
            
            // Find existing result or create new one for analyte-based tests
            Result result = findExistingResult(resultDAO, analysis, testAnalytes.get(0));
            
            boolean isNewResult = (result == null);
            if (isNewResult) {
                result = new Result();
                result.setAnalysis(analysis);
                result.setAnalyte(testAnalytes.get(0).getAnalyte());
                result.setSortOrder(testAnalytes.get(0).getSortOrder());
            }

            // Set result data
            result.setTestResult(testResult);
            result.setValue(normalizedValue);
            result.setResultType(normalizedType);
            result.setIsReportable("N"); // Match UI behavior - set to N
            result.setSortOrder("0"); // Match UI behavior - always use 0
            result.setSysUserId(sysUserId);

            // ✅ FIX: Add reference range population (this was missing!)
            // Get patient for reference range calculation
            SampleHumanDAO sampleHumanDAO = new SampleHumanDAOImpl();
            Patient patient = sampleHumanDAO.getPatientForSample(sample);

            // Get result limits (reference ranges) for this test and patient
            ResultsLoadUtility resultsLoadUtility = new ResultsLoadUtility();
            ResultLimit resultLimit = resultsLoadUtility.getResultLimitForTestAndPatient(test, patient);

            // Set reference ranges (this is what UI does but REST was missing!)
            result.setMinNormal(resultLimit.getLowNormal());
            result.setMaxNormal(resultLimit.getHighNormal());
            String resultLimitId = resultLimit.getId();
            result.setResultLimitId(!StringUtil.isNullorNill(resultLimitId) ? Integer.parseInt(resultLimitId) : null);

            // Save using proper DAO (this triggers audit trails, validation, etc.)
            if (isNewResult) {
                resultDAO.insertData(result);
            } else {
                resultDAO.updateData(result);
            }

            // Add result signature (align with UI behavior)
            addResultSignature(result.getId(), sysUserId);
            
            // Update sample status to "Testing finished" (3) to match UI behavior
            updateSampleStatusToTestingFinished(analysis, sysUserId);

            // Update analysis status to match UI flow - set to "Technical Acceptance" for validation
            // This follows the exact same pattern as ResultsEntryUpdateAction.java lines 562-563
            Analysis freshAnalysis = new Analysis();
            freshAnalysis.setId(analysisId);
            analysisDAO.getData(freshAnalysis);
            freshAnalysis.setSysUserId(sysUserId);
            
            // Set statusId to "Technical Acceptance" like UI validation workflow expects
            // The validation workflow checks statusId field, not status field
            freshAnalysis.setStatusId(StatusOfSampleUtil.getStatusID(StatusOfSampleUtil.AnalysisStatus.TechnicalAcceptance));
            
            // MATCH UI EXACTLY: Set all the same fields as updateAndAddAnalysisToModifiedList()
            String currentDateString = DateUtil.convertSqlDateToStringDate(DateUtil.getNowAsSqlDate());
            freshAnalysis.setStartedDateForDisplay(currentDateString); // UI sets this
            freshAnalysis.setCompletedDate(DateUtil.convertStringDateToSqlDate(currentDateString));
            
            // Increment revision like UI does
            freshAnalysis.setRevision(String.valueOf(Integer.parseInt(freshAnalysis.getRevision()) + 1));
            // Set entry date like UI does (returns java.sql.Timestamp)
            freshAnalysis.setEnteredDate(DateUtil.getNowAsTimestamp());
            
            analysisDAO.updateData(freshAnalysis);

            // Build success response
            ResultUpdateResponse response = new ResultUpdateResponse(true, "Test result updated successfully - requires validation before finalization");
            response.analysisId = analysisId;
            response.testId = testId;
            response.testName = test.getTestName();
            response.accessionNumber = sample.getAccessionNumber();
            response.resultValue = normalizedValue;
            response.resultType = normalizedType;
            response.statusUpdated = true;
            response.rowsAffected = 1;
            
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return new ResultUpdateResponse(false, "Error updating test result: " + e.getMessage());
        }
    }

    private ResultUpdateResponse updateDirectResult(Analysis analysis, Test test, TestResult testResult, String resultValue, String resultType, String sysUserId, ResultDAO resultDAO) {
        try {
            // Create a result directly linked to the analysis without analyte
            Result result = new Result();
            result.setAnalysis(analysis);
            result.setTestResult(testResult);
            result.setValue(resultValue);
            result.setSortOrder("0"); // Match UI behavior - always use 0
            result.setSysUserId(sysUserId);
            result.setIsReportable("N"); // Match UI behavior - set to N
            result.setResultType(resultType);
            result.setLastupdated(new Timestamp(System.currentTimeMillis()));
            
            // ✅ FIX: Add reference range population for direct results too
            // Get patient for reference range calculation
            SampleHumanDAO sampleHumanDAO = new SampleHumanDAOImpl();
            SampleItem sampleItem = analysis.getSampleItem();
            Sample sample = sampleItem != null ? sampleItem.getSample() : null;
            if (sample != null) {
                Patient patient = sampleHumanDAO.getPatientForSample(sample);
                
                // Get result limits (reference ranges) for this test and patient
                ResultsLoadUtility resultsLoadUtility = new ResultsLoadUtility();
                ResultLimit resultLimit = resultsLoadUtility.getResultLimitForTestAndPatient(test, patient);
                
                // Set reference ranges (this is what UI does but REST was missing!)
                result.setMinNormal(resultLimit.getLowNormal());
                result.setMaxNormal(resultLimit.getHighNormal());
                String resultLimitId = resultLimit.getId();
                result.setResultLimitId(!StringUtil.isNullorNill(resultLimitId) ? Integer.parseInt(resultLimitId) : null);
            }
            
            // Insert or update existing result for this analysis (if present)
            List existing = resultDAO.getResultsByAnalysis(analysis);
            if (existing != null && !existing.isEmpty()) {
                Result existingResult = (Result) existing.get(0);
                result.setId(existingResult.getId());
                resultDAO.updateData(result);
            } else {
                resultDAO.insertData(result);
            }

            // Add result signature
            addResultSignature(result.getId(), sysUserId);
            
            // Update sample status to "Testing finished" (3) to match UI behavior  
            updateSampleStatusToTestingFinished(analysis, sysUserId);

            // Update analysis status to match UI flow - set to "Technical Acceptance" for validation
            AnalysisDAO analysisDAO = new AnalysisDAOImpl();
            Analysis freshAnalysis = new Analysis();
            freshAnalysis.setId(analysis.getId());
            analysisDAO.getData(freshAnalysis);
            freshAnalysis.setSysUserId(sysUserId);
            
            // Set statusId to "Technical Acceptance" like UI validation workflow expects
            // The validation workflow checks statusId field, not status field
            freshAnalysis.setStatusId(StatusOfSampleUtil.getStatusID(StatusOfSampleUtil.AnalysisStatus.TechnicalAcceptance));
            
            // MATCH UI EXACTLY: Set all the same fields as updateAndAddAnalysisToModifiedList()
            String currentDateString = DateUtil.convertSqlDateToStringDate(DateUtil.getNowAsSqlDate());
            freshAnalysis.setStartedDateForDisplay(currentDateString); // UI sets this
            freshAnalysis.setCompletedDate(DateUtil.convertStringDateToSqlDate(currentDateString));
            
            // Increment revision like UI does
            freshAnalysis.setRevision(String.valueOf(Integer.parseInt(freshAnalysis.getRevision()) + 1));
            // Set entry date like UI does (returns java.sql.Timestamp)
            freshAnalysis.setEnteredDate(DateUtil.getNowAsTimestamp());
            
            analysisDAO.updateData(freshAnalysis);

            // Build success response with details
            SampleItem si = analysis.getSampleItem();
            Sample responseSample = si != null ? si.getSample() : null;

            ResultUpdateResponse response = new ResultUpdateResponse(true, "Test result updated successfully - requires validation before finalization");
            response.analysisId = analysis.getId();
            response.testId = test != null ? test.getId() : null;
            response.testName = test != null ? test.getTestName() : null;
            response.accessionNumber = responseSample != null ? responseSample.getAccessionNumber() : null;
            response.resultValue = resultValue;
            response.resultType = resultType;
            response.statusUpdated = true;
            response.rowsAffected = 1;
        
            // Note: No atomfeed event published for status 3 (Technical Acceptance)
            // Atomfeed events are only published when results are finalized (status 6)
            // This matches UI behavior where validation is required before Bahmni sync
        
            return response;
            
        } catch (Exception e) {
            e.printStackTrace();
            return new ResultUpdateResponse(false, "Error updating direct result: " + e.getMessage());
        }
    }
    
    /**
     * Publish atomfeed event for Bahmni synchronization
     * This ensures REST updates trigger the same atomfeed events as UI updates
     * Only publishes events for finalized results (status 6) - matching UI behavior
     * 
     * Note: This method is no longer called for REST result entry since we now
     * follow UI flow and set status to 3 (Technical Acceptance) which requires
     * manual validation before finalization and Bahmni sync.
     */
    private void publishAtomfeedEvent(Analysis analysis, String contextPath) {
        try {
            // Only publish atomfeed events for released analyses (status_id = 6)
            // This matches the UI behavior where only "released" results sync to Bahmni
            if (!"6".equals(analysis.getStatusId())) {
                System.out.println("Skipping atomfeed event - analysis not released (status: " + analysis.getStatusId() + ")");
                return;
            }
            
            OpenElisUrlPublisher accessionPublisher = new EventPublishers().accessionPublisher();
            Sample sample = analysis.getSampleItem().getSample();
            java.util.List<String> editedSampleUuids = Arrays.asList(sample.getUUID());
            accessionPublisher.publish(editedSampleUuids, contextPath);
            System.out.println("Published atomfeed event for completed analysis - sample UUID: " + sample.getUUID());
        } catch (Exception e) {
            // Log but don't fail the result update
            System.err.println("Failed to publish atomfeed event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper to resolve TestResult and normalized value/type according to UI rules
    private ResolvedResult resolveTestResultAndValue(Test test, TestResultDAO testResultDAO, String incomingValue, String incomingType) {
        ResolvedResult rr = new ResolvedResult();
        try {
            String type = incomingType;
            if (type == null || type.trim().isEmpty()) {
                // Default to remark if not provided
                type = "R";
            }

            if ("D".equalsIgnoreCase(type)) {
                // Map dictionary entry text to dictionary ID and matching TestResult
                DictionaryDAO dictDAO = new DictionaryDAOImpl();
                Dictionary dict = dictDAO.getDictionaryByDictEntry(incomingValue);
                if (dict == null) {
                    rr.success = false;
                    rr.message = "Dictionary entry not found for value: " + incomingValue;
                    return rr;
                }
                List<TestResult> trs = testResultDAO.getAllTestResultsPerTest(test);
                TestResult match = null;
                if (trs != null) {
                    for (TestResult tr : trs) {
                        if ("D".equalsIgnoreCase(tr.getTestResultType()) && dict.getId().equals(tr.getValue())) {
                            match = tr;
                            break;
                        }
                    }
                }
                if (match == null) {
                    rr.success = false;
                    rr.message = "No matching TestResult configured for dictionary value: " + incomingValue;
                    return rr;
                }
                rr.testResult = match;
                rr.value = dict.getId();
                rr.type = "D";
                rr.success = true;
                return rr;
            } else if ("R".equalsIgnoreCase(type)) {
                // Find remark-type TestResult for this test
                List<TestResult> trs = testResultDAO.getAllTestResultsPerTest(test);
                TestResult remark = null;
                if (trs != null) {
                    for (TestResult tr : trs) {
                        if ("R".equalsIgnoreCase(tr.getTestResultType())) {
                            remark = tr;
                            break;
                        }
                    }
                }
                if (remark == null) {
                    rr.success = false;
                    rr.message = "No remark TestResult configured for test: " + test.getTestName();
                    return rr;
                }
                rr.testResult = remark;
                rr.value = incomingValue;
                rr.type = "R";
                rr.success = true;
                return rr;
            } else if ("N".equalsIgnoreCase(type)) {
                // Numeric value, no specific TestResult required
                rr.testResult = null;
                rr.value = incomingValue;
                rr.type = "N";
                rr.success = true;
                return rr;
            } else {
                rr.success = false;
                rr.message = "Unsupported resultType: " + type;
                return rr;
            }
        } catch (Exception e) {
            rr.success = false;
            rr.message = "Error resolving result: " + e.getMessage();
            return rr;
        }
    }

    private void addResultSignature(String resultId, String sysUserId) {
        try {
            SystemUser systemUser = new SystemUserDAOImpl().getUserById(sysUserId);
            ResultSignature signature = new ResultSignature();
            signature.setIsSupervisor(false);
            signature.setResultId(resultId);
            signature.setNonUserName("Open ELIS"); // Match UI behavior exactly - with space
            signature.setSystemUser(systemUser);
            new ResultSignatureDAOImpl().insertData(signature);
        } catch (Exception ignore) {
            // Do not fail the update if signature creation fails
        }
    }
    
    /**
     * Update sample status to "Testing finished" (3) to match UI behavior
     * This is critical for Bahmni visibility as many queries filter by sample status
     */
    private void updateSampleStatusToTestingFinished(Analysis analysis, String sysUserId) {
        try {
            SampleDAO sampleDAO = new SampleDAOImpl();
            Sample sample = analysis.getSampleItem().getSample();
            
            // Get fresh sample data
            Sample freshSample = new Sample();
            freshSample.setId(sample.getId());
            sampleDAO.getData(freshSample);
            
            // Update to "Testing finished" status (3) to match UI behavior
            freshSample.setStatusId("3"); // Testing finished
            freshSample.setSysUserId(sysUserId);
            
            sampleDAO.updateData(freshSample);
            System.out.println("Updated sample status to 'Testing finished' (3) for sample: " + sample.getAccessionNumber());
        } catch (Exception e) {
            // Log but don't fail the result update
            System.err.println("Failed to update sample status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class ResolvedResult {
        boolean success;
        String message;
        TestResult testResult;
        String value;
        String type;
    }

    private Result findExistingResult(ResultDAO resultDAO, Analysis analysis, TestAnalyte testAnalyte) {
        try {
            Result result = new Result();
            resultDAO.getResultByAnalysisAndAnalyte(result, analysis, testAnalyte);
            return result.getId() != null ? result : null;
        } catch (Exception e) {
            // Result doesn't exist, will create new one
            return null;
        }
    }
}
