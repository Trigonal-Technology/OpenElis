/*
* The contents of this file are subject to the Mozilla Public License
* Version 1.1 (the "License"); you may not use this file except in
* compliance with the License. You may obtain a copy of the License at
* http://www.mozilla.org/MPL/ 
* 
* Software distributed under the License is distributed on an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
* License for the specific language governing rights and limitations under
* the License.
* 
* The Original Code is OpenELIS code.
* 
* Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
*/

package org.bahmni.feed.openelis.feed.service.impl;

import org.bahmni.feed.openelis.feed.contract.odoo.OdooTest;
import org.bahmni.feed.openelis.externalreference.dao.ExternalReferenceDao;
import org.bahmni.feed.openelis.externalreference.daoimpl.ExternalReferenceDaoImpl;
import org.bahmni.feed.openelis.externalreference.valueholder.ExternalReference;
import org.bahmni.feed.openelis.feed.contract.bahmnireferencedata.MinimalResource;
import org.bahmni.feed.openelis.feed.contract.bahmnireferencedata.ReferenceDataTest;
import org.bahmni.feed.openelis.utils.AuditingService;
import org.hibernate.Transaction;
import us.mn.state.health.lims.common.action.IActionConstants;
import us.mn.state.health.lims.common.exception.LIMSException;
import us.mn.state.health.lims.dictionary.daoimpl.DictionaryDAOImpl;
import us.mn.state.health.lims.dictionary.valueholder.Dictionary;
import us.mn.state.health.lims.hibernate.HibernateUtil;
import us.mn.state.health.lims.login.daoimpl.LoginDAOImpl;
import us.mn.state.health.lims.siteinformation.daoimpl.SiteInformationDAOImpl;
import us.mn.state.health.lims.test.dao.TestDAO;
import us.mn.state.health.lims.test.dao.TestSectionDAO;
import us.mn.state.health.lims.test.daoimpl.TestDAOImpl;
import us.mn.state.health.lims.test.daoimpl.TestSectionDAOImpl;
import us.mn.state.health.lims.test.valueholder.Test;
import us.mn.state.health.lims.dictionary.dao.DictionaryDAO;
import us.mn.state.health.lims.test.valueholder.TestSection;
import us.mn.state.health.lims.typeofsample.dao.TypeOfSampleDAO;
import us.mn.state.health.lims.typeofsample.dao.TypeOfSampleTestDAO;
import us.mn.state.health.lims.typeofsample.daoimpl.TypeOfSampleDAOImpl;
import us.mn.state.health.lims.typeofsample.daoimpl.TypeOfSampleTestDAOImpl;
import us.mn.state.health.lims.typeofsample.util.TypeOfSampleUtil;
import us.mn.state.health.lims.typeofsample.valueholder.TypeOfSample;
import us.mn.state.health.lims.typeofsample.valueholder.TypeOfSampleTest;
import us.mn.state.health.lims.panel.dao.PanelDAO;
import us.mn.state.health.lims.panel.daoimpl.PanelDAOImpl;
import us.mn.state.health.lims.panel.valueholder.Panel;
import us.mn.state.health.lims.panelitem.dao.PanelItemDAO;
import us.mn.state.health.lims.panelitem.daoimpl.PanelItemDAOImpl;
import us.mn.state.health.lims.panelitem.valueholder.PanelItem;
import org.bahmni.feed.openelis.feed.contract.bahmnireferencedata.CodedTestAnswer;
import us.mn.state.health.lims.typeofsample.dao.TypeOfSamplePanelDAO;
import us.mn.state.health.lims.typeofsample.daoimpl.TypeOfSamplePanelDAOImpl;
import us.mn.state.health.lims.typeofsample.valueholder.TypeOfSamplePanel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

public class TestService {
    private static final Logger logger = LogManager.getLogger(TestService.class);

    public static final String CATEGORY_TEST = "Test";
    public static final String CATEGORY_PANEL = "Panel";
    public static final String CATEGORY_TEST_CODED_ANS = "CodedAns";
    public static final String DUMMY_TEST_SECTION_NAME = "New";
    private DictionaryDAO dictionaryDao;
    private AuditingService auditingService;
    private TestDAO testDAO;
    private TestResultService testResultService;
    private ExternalReferenceDao externalReferenceDao;
    private TestSectionDAO testSectionDAO;
    private UnitOfMeasureService unitOfMeasureService;
    private TypeOfSampleDAO typeOfSampleDAO;
    private TypeOfSampleTestDAO typeOfSampleTestDAO;
    private TypeOfSamplePanelDAO typeOfSamplePanelDAO;
    private PanelDAO panelDAO;
    private PanelItemDAO panelItemDAO;

    public TestService() {
        this.testDAO = new TestDAOImpl();
        this.testResultService = new TestResultService();
        this.externalReferenceDao = new ExternalReferenceDaoImpl();
        this.testSectionDAO = new TestSectionDAOImpl();
        this.auditingService = new AuditingService(new LoginDAOImpl(), new SiteInformationDAOImpl());
        this.unitOfMeasureService = new UnitOfMeasureService();
        this.dictionaryDao = new DictionaryDAOImpl();
        this.typeOfSampleDAO = new TypeOfSampleDAOImpl();
        this.typeOfSampleTestDAO = new TypeOfSampleTestDAOImpl();
        this.typeOfSamplePanelDAO = new TypeOfSamplePanelDAOImpl();
        this.panelDAO = new PanelDAOImpl();
        this.panelItemDAO = new PanelItemDAOImpl();
    }

    /**
     * Exposed a constructor which takes all params for unit-testing purposes.
     */
    public TestService(ExternalReferenceDao externalReferenceDao,
            TestDAO testDAO,
            TestResultService testResultService,
            TestSectionDAO testSectionDAO,
            AuditingService auditingService,
            TypeOfSampleDAO typeOfSampleDAO,
            TypeOfSampleTestDAO typeOfSampleTestDAO,
            DictionaryDAO dictionaryDao) {

        this.externalReferenceDao = externalReferenceDao;
        this.testDAO = testDAO;
        this.testResultService = testResultService;
        this.testSectionDAO = testSectionDAO;
        this.auditingService = auditingService;
        this.dictionaryDao = dictionaryDao;

    }

    public void createOrUpdate(ReferenceDataTest referenceDataTest) throws IOException, LIMSException {
        try {
            String sysUserId = auditingService.getSysUserId();
            ExternalReference data = externalReferenceDao.getData(referenceDataTest.getId(), CATEGORY_TEST);
            Test test = new Test();

            if (data == null) {
                if (referenceDataTest.getIsActive()) {
                    test = populateTest(test, referenceDataTest, sysUserId, null);
                    testDAO.insertData(test);
                    saveExternalReference(referenceDataTest, test);
                } else {
                    return;
                }
            } else {
                test = testDAO.getTestById(String.valueOf(data.getItemId()));
                String uuid = test.getTestSection() != null ? test.getTestSection().getUUID() : null;
                populateTest(test, referenceDataTest, sysUserId, uuid);
                testDAO.updateData(test);
            }
            if (referenceDataTest.getResultType().equals("Text")) {
                testResultService.createOrUpdate(test, "R", null);
            }
            if (referenceDataTest.getResultType().equals("Coded")) {
                Collection<CodedTestAnswer> codedTestAnswer = (Collection<CodedTestAnswer>) referenceDataTest
                        .getCodedTestAnswer();
                Dictionary dict = null;
                // Mark all coded Ans inactive
                testResultService.makeCodedAnswersInactive(test.getId());
                for (CodedTestAnswer testAnswer : codedTestAnswer) {
                    ExternalReference dictReference = externalReferenceDao.getData(testAnswer.getUuid(),
                            CATEGORY_TEST_CODED_ANS);
                    if (dictReference == null) {
                        Dictionary existingDictionary = dictionaryDao.getDictionaryByDictEntry(testAnswer.getName());
                        if (existingDictionary == null) {
                            dict = new Dictionary();
                            dict.setDictEntry(testAnswer.getName());
                            dict.setLastupdated(new Timestamp(new Date().getTime()));
                            dict.setSysUserId(sysUserId);
                            dictionaryDao.insertData(dict);
                        } else {
                            dict = existingDictionary;
                        }
                        saveExternalReference(testAnswer, dict);
                    } else {
                        dict = dictionaryDao.getDictionaryById(String.valueOf(dictReference.getItemId()));
                        dict.setDictEntry(testAnswer.getName());
                        dict.setLastupdated(new Timestamp(new Date().getTime()));
                        dict.setSysUserId(sysUserId);
                        dictionaryDao.updateData(dict, false);
                    }
                    // If there is existing rln make it active
                    testResultService.createOrUpdate(test, "D", dict.getId());
                }
            }
            TypeOfSampleUtil.clearTestCache();
        } catch (Exception e) {
            throw new LIMSException(String.format("Error while saving test - %s", referenceDataTest.getName()), e);
        }
    }

    private void saveExternalReference(CodedTestAnswer codedTestAnswer, Dictionary dict) {
        ExternalReference data;
        data = new ExternalReference(Long.parseLong(dict.getId()), codedTestAnswer.getUuid(), CATEGORY_TEST_CODED_ANS);
        externalReferenceDao.insertData(data);
    }

    private void saveExternalReference(ReferenceDataTest referenceDataTest, Test test) {
        ExternalReference data;
        data = new ExternalReference(Long.parseLong(test.getId()), referenceDataTest.getId(), CATEGORY_TEST);
        externalReferenceDao.insertData(data);
    }

    private Test populateTest(Test test, ReferenceDataTest referenceDataTest, String sysUserId, String testSectionUuid)
            throws IOException {
        test.setTestName(referenceDataTest.getName());
        // Assign to dummy test section
        TestSection section = getTestSection(testSectionUuid);
        if (referenceDataTest.getTestUnitOfMeasure() != null) {
            test.setUnitOfMeasure(unitOfMeasureService.create(referenceDataTest.getTestUnitOfMeasure()));
        }
        test.setTestSection(section);
        test.setDescription(referenceDataTest.getName());
        test.setIsActive(referenceDataTest.getIsActive() ? IActionConstants.YES : IActionConstants.NO);
        test.setLastupdated(new Timestamp(new Date().getTime()));
        test.setName(referenceDataTest.getName());
        test.setReferenceInfo(referenceDataTest.getReferenceInfo());
        test.setSysUserId(sysUserId);
        test.setOrderable(true);
        test.setSortOrder(String.valueOf(referenceDataTest.getSortOrder()));
        return test;
    }

    private TestSection getTestSection(String uuid) {
        if (uuid == null) {
            return testSectionDAO.getTestSectionByName(DUMMY_TEST_SECTION_NAME);
        } else {
            return testSectionDAO.getTestSectionByUUID(uuid);
        }
    }

    public Test updateTestSection(String testName, String testSectionUuid, String sysUserId) throws LIMSException {
        Test test = testDAO.getTestByName(testName);
        if (test == null) {
            throw new LIMSException(String.format("%s test does not exist", testName));
        }
        test.setSysUserId(sysUserId);
        TestSection testSection = getTestSection(testSectionUuid);
        if (!test.getTestSection().equals(testSection)) {
            test.setTestSection(testSection);
            testDAO.updateData(test);
        }
        return test;
    }

    public Test getTest(MinimalResource test) {
        return testDAO.getTestByName(test.getName());
    }

    public void createOrUpdateFromOdoo(OdooTest odooTest) throws LIMSException {
        logger.info("=== Starting createOrUpdateFromOdoo ===");
        logger.info("Test ID: {}", odooTest.getId());
        logger.info("Test Name: {}", odooTest.getName());
        logger.info("Department: {}", odooTest.getDepartment());
        logger.info("Sample Type: {}", odooTest.getSampleType());
        logger.info("Result Type: {}", odooTest.getResultType());
        logger.info("Is Panel: {}", odooTest.getIsPanel());
        logger.info("Active: {}", odooTest.getActive());

        try {
            logger.info("Getting system user ID...");
            String sysUserId = auditingService.getSysUserId();
            logger.info("System User ID: {}", sysUserId);

            if (odooTest.getIsPanel()) {
                logger.info("Processing as panel...");
                syncPanelFromOdoo(odooTest, sysUserId);
                return;
            }

            // Try to find by external reference first
            logger.info("Checking for existing external reference...");
            ExternalReference data = externalReferenceDao.getData(String.valueOf(odooTest.getId()), CATEGORY_TEST);
            Test test = null;

            if (data != null) {
                logger.info("Found external reference, loading test by ID: {}", data.getItemId());
                test = testDAO.getTestById(String.valueOf(data.getItemId()));
            } else {
                // Try to find by name if external reference doesn't exist
                logger.info("No external reference found, searching by name: {}", odooTest.getName());
                test = testDAO.getTestByName(odooTest.getName());
            }

            if (test == null) {
                logger.info("Test not found, creating new test...");
                if (odooTest.getActive()) {
                    test = new Test();
                    logger.info("Populating test data...");
                    populateTestFromOdoo(test, odooTest, sysUserId);
                    logger.info("Inserting test into database...");
                    testDAO.insertData(test);
                    logger.info("Test inserted with ID: {}", test.getId());
                    // Save external reference
                    logger.info("Creating external reference...");
                    ExternalReference ref = new ExternalReference(Long.parseLong(test.getId()),
                            String.valueOf(odooTest.getId()), CATEGORY_TEST);
                    externalReferenceDao.insertData(ref);
                    logger.info("External reference created");
                } else {
                    logger.info("Test is inactive, skipping creation");
                }
            } else {
                logger.info("Test found, updating existing test ID: {}", test.getId());
                populateTestFromOdoo(test, odooTest, sysUserId);
                logger.info("Updating test in database...");
                testDAO.updateData(test);
                logger.info("Test updated successfully");
            }

            // Simple result type handling (Numerical/Text)
            if (test != null && odooTest.getResultType() != null) {
                logger.info("Processing result type: {}", odooTest.getResultType());
                if (odooTest.getResultType().equalsIgnoreCase("numerical") ||
                        odooTest.getResultType().equalsIgnoreCase("text")) {
                    logger.info("Creating/updating test result...");
                    testResultService.createOrUpdate(test, "R", null);
                    logger.info("Test result processed");
                }
            }

            logger.info("Clearing test cache...");
            TypeOfSampleUtil.clearTestCache();
            logger.info("=== Test sync completed successfully ===");
        } catch (Exception e) {
            logger.error("=== Error in createOrUpdateFromOdoo ===", e);
            logger.error("Error details: {}", e.getMessage());
            logger.error("Stack trace:", e);
            throw new LIMSException(String.format("Error while saving test from Odoo - %s", odooTest.getName()), e);
        }
    }

    private Test populateTestFromOdoo(Test test, OdooTest odooTest, String sysUserId) throws IOException {
        logger.info("=== Populating test from Odoo data ===");
        test.setTestName(odooTest.getName());
        test.setName(odooTest.getName());
        test.setDescription(
                odooTest.getDescription() != null && !odooTest.getDescription().isEmpty() ? odooTest.getDescription()
                        : odooTest.getName());
        test.setIsActive(
                odooTest.getActive() != null && odooTest.getActive() ? IActionConstants.YES : IActionConstants.NO);
        test.setLastupdated(new Timestamp(new Date().getTime()));
        test.setSysUserId(sysUserId);
        test.setOrderable(true);
        test.setSortOrder(odooTest.getSortOrder() != null ? String.valueOf(odooTest.getSortOrder()) : "0");
        test.setReferenceInfo(odooTest.getReferenceRange());
        test.setLoinc(odooTest.getLoinc());
        logger.info("Basic test fields set");

        // Assign to department (Test Section) if provided - CREATE if it doesn't exist
        if (odooTest.getDepartment() != null && !odooTest.getDepartment().isEmpty()) {
            logger.info("Processing department: {}", odooTest.getDepartment());

            // Log current test section if exists
            if (test.getTestSection() != null) {
                logger.info("Current test section: {} (ID: {})",
                        test.getTestSection().getTestSectionName(),
                        test.getTestSection().getId());
            } else {
                logger.info("Test currently has no test section assigned");
            }

            TestSection section = testSectionDAO.getTestSectionByName(odooTest.getDepartment());
            if (section == null) {
                logger.info("Department not found, creating new test section: {}", odooTest.getDepartment());
                section = new TestSection();
                section.setTestSectionName(odooTest.getDepartment());
                section.setDescription(odooTest.getDepartment()); // Set description (required field)
                section.setIsActive(IActionConstants.YES);
                section.setLastupdated(new Timestamp(new Date().getTime()));
                section.setSysUserId(sysUserId);
                testSectionDAO.insertData(section);
                logger.info("Test section created with ID: {}", section.getId());
            } else {
                logger.info("Found existing test section with ID: {}", section.getId());
            }
            test.setTestSection(section);
            logger.info("Test section set to: {} (ID: {})", section.getTestSectionName(), section.getId());
        } else {
            logger.info("No department provided in Odoo data");
        }

        // Assign to default test section if not set
        if (test.getTestSection() == null) {
            logger.info("No department specified, using default test section");
            test.setTestSection(getTestSection(null));
        }

        // Set Unit of Measure
        if (odooTest.getUom() != null && !odooTest.getUom().isEmpty()) {
            logger.info("Setting unit of measure: {}", odooTest.getUom());
            test.setUnitOfMeasure(unitOfMeasureService.create(odooTest.getUom()));
        }

        // Link to Sample Type so it appears in Collect Sample section
        if (odooTest.getSampleType() != null && !odooTest.getSampleType().isEmpty()) {
            logger.info("Linking to sample type: {}", odooTest.getSampleType());
            linkTestToSampleType(test, odooTest.getSampleType(), sysUserId);
        }

        logger.info("=== Test population completed ===");
        return test;
    }

    private void linkTestToSampleType(Test test, String sampleTypeName, String sysUserId) {
        logger.info("Attempting to link test '{}' (ID: {}) to sample type '{}'",
                test.getTestName(), test.getId(), sampleTypeName);
        try {
            TypeOfSample tosParam = new TypeOfSample();
            tosParam.setDescription(sampleTypeName);
            // Search ignoring case
            TypeOfSample tos = typeOfSampleDAO.getTypeOfSampleByDescriptionAndDomain(tosParam, true);
            if (tos != null) {
                logger.info("Found sample type '{}' with ID: {}", tos.getDescription(), tos.getId());
                List<TypeOfSampleTest> existingLinks = typeOfSampleTestDAO.getTypeOfSampleTestsForTest(test.getId());
                logger.info("Test has {} existing sample type links", existingLinks != null ? existingLinks.size() : 0);
                boolean found = false;
                for (TypeOfSampleTest link : existingLinks) {
                    if (link.getTypeOfSampleId().equals(tos.getId())) {
                        found = true;
                        logger.info("Link already exists between test and sample type");
                        break;
                    }
                }
                if (!found) {
                    logger.info("Creating new link between test {} and sample type {}", test.getId(), tos.getId());
                    TypeOfSampleTest newLink = new TypeOfSampleTest();
                    newLink.setTestId(test.getId());
                    newLink.setTypeOfSampleId(tos.getId());
                    newLink.setSysUserId(sysUserId);
                    typeOfSampleTestDAO.insertData(newLink);
                    logger.info("Successfully linked test '{}' to sample type '{}'", test.getTestName(),
                            sampleTypeName);
                } else {
                    logger.info("Test '{}' is already linked to sample type '{}'", test.getTestName(), sampleTypeName);
                }
            } else {
                logger.warn(
                        "Sample type '{}' not found in OpenELIS. Cannot link test '{}'. Available sample types should be created first.",
                        sampleTypeName, test.getTestName());
            }
        } catch (Exception e) {
            logger.error("Error linking test '{}' to sample type '{}': {}",
                    test.getTestName(), sampleTypeName, e.getMessage(), e);
        }
    }

    private void linkPanelToSampleType(Panel panel, String sampleTypeName, String sysUserId) {
        try {
            logger.info("Attempting to link panel {} to sample type {}", panel.getPanelName(), sampleTypeName);
            TypeOfSample tosParam = new TypeOfSample();
            tosParam.setDescription(sampleTypeName);
            // Search ignoring case
            TypeOfSample tos = typeOfSampleDAO.getTypeOfSampleByDescriptionAndDomain(tosParam, true);
            if (tos != null) {
                logger.info("Found sample type with ID: {}", tos.getId());
                // Check if link already exists
                TypeOfSamplePanel existingLink = typeOfSamplePanelDAO.getTypeOfSamplePanelForPanel(panel.getId());
                if (existingLink == null || !existingLink.getTypeOfSampleId().equals(tos.getId())) {
                    logger.info("Creating new link between panel and sample type");
                    TypeOfSamplePanel newLink = new TypeOfSamplePanel();
                    newLink.setPanelId(panel.getId());
                    newLink.setTypeOfSampleId(tos.getId());
                    newLink.setSysUserId(sysUserId);
                    typeOfSamplePanelDAO.insertData(newLink);
                    logger.info("Panel successfully linked to sample type");
                } else {
                    logger.info("Link already exists between panel and sample type");
                }
            } else {
                logger.warn("Sample type not found: {}", sampleTypeName);
            }
        } catch (Exception e) {
            logger.error("Error linking panel to sample type", e);
        }
    }

    private void syncPanelFromOdoo(OdooTest odooTest, String sysUserId) throws LIMSException {
        logger.info("=== Syncing panel from Odoo: {} ===", odooTest.getName());
        try {
            ExternalReference data = externalReferenceDao.getData(String.valueOf(odooTest.getId()), CATEGORY_PANEL);
            Panel panel = null;
            if (data != null) {
                logger.info("Found external reference for panel, loading by ID: {}", data.getItemId());
                panel = panelDAO.getPanelById(String.valueOf(data.getItemId()));
            } else {
                logger.info("No external reference, searching panel by name: {}", odooTest.getName());
                panel = panelDAO.getPanelByName(odooTest.getName());
            }

            if (panel == null) {
                logger.info("Panel not found, creating new panel");
                if (odooTest.getActive()) {
                    panel = new Panel();
                    populatePanelFromOdoo(panel, odooTest, sysUserId);
                    panelDAO.insertData(panel);
                    logger.info("Panel created with ID: {}", panel.getId());
                    ExternalReference ref = new ExternalReference(Long.parseLong(panel.getId()),
                            String.valueOf(odooTest.getId()), CATEGORY_PANEL);
                    externalReferenceDao.insertData(ref);
                    logger.info("External reference created for panel");
                }
            } else {
                logger.info("Panel found, updating existing panel ID: {}", panel.getId());
                populatePanelFromOdoo(panel, odooTest, sysUserId);
                panelDAO.updateData(panel);
                logger.info("Panel updated");
            }

            if (panel != null) {
                logger.info("Syncing panel items...");
                syncPanelItems(panel, odooTest, sysUserId);

                // Link panel to sample type so it appears in Collect Sample section
                String sampleType = odooTest.getSampleType();

                // If panel doesn't have explicit sample type, infer from first test in panel
                if (sampleType == null || sampleType.isEmpty()) {
                    logger.info("No explicit sample type for panel, checking component tests...");
                    List<PanelItem> panelItems = panelItemDAO.getPanelItemsForPanel(panel.getId());
                    if (panelItems != null && !panelItems.isEmpty()) {
                        // Get the first test's sample type
                        PanelItem firstItem = panelItems.get(0);
                        Test firstTest = testDAO.getTestById(firstItem.getTest().getId());
                        if (firstTest != null) {
                            List<TypeOfSampleTest> sampleLinks = typeOfSampleTestDAO
                                    .getTypeOfSampleTestsForTest(firstTest.getId());
                            if (sampleLinks != null && !sampleLinks.isEmpty()) {
                                TypeOfSample tos = typeOfSampleDAO
                                        .getTypeOfSampleById(sampleLinks.get(0).getTypeOfSampleId());
                                if (tos != null) {
                                    sampleType = tos.getDescription();
                                    logger.info("Inferred sample type from first test: {}", sampleType);
                                }
                            }
                        }
                    }
                }

                if (sampleType != null && !sampleType.isEmpty()) {
                    logger.info("Linking panel to sample type: {}", sampleType);
                    linkPanelToSampleType(panel, sampleType, sysUserId);
                } else {
                    logger.info("No sample type available for panel (neither explicit nor inferred from tests)");
                }
            }
            logger.info("=== Panel sync completed ===");
        } catch (Exception e) {
            logger.error("Error syncing panel from Odoo", e);
            throw new LIMSException(String.format("Error while saving panel from Odoo - %s", odooTest.getName()), e);
        }
    }

    private void populatePanelFromOdoo(Panel panel, OdooTest odooTest, String sysUserId) {
        panel.setPanelName(odooTest.getName());
        panel.setDescription(odooTest.getName());
        panel.setSysUserId(sysUserId);
        panel.setIsActive(
                odooTest.getActive() != null && odooTest.getActive() ? IActionConstants.YES : IActionConstants.NO);
        panel.setLastupdated(new Timestamp(new Date().getTime()));
        panel.setSortOrderInt(odooTest.getSortOrder() != null ? odooTest.getSortOrder() : 0);
    }

    private void syncPanelItems(Panel panel, OdooTest odooTest, String sysUserId) throws LIMSException {
        // Delete existing items
        List<PanelItem> items = panelItemDAO.getPanelItemByPanel(panel, false);
        for (PanelItem item : items) {
            item.setSysUserId(sysUserId);
        }
        panelItemDAO.deleteData(items);

        // Add new items
        if (odooTest.getTestUuids() != null) {
            int sortOrder = 1;
            for (String testUuid : odooTest.getTestUuids()) {
                ExternalReference ref = externalReferenceDao.getData(testUuid, CATEGORY_TEST);
                if (ref != null) {
                    Test test = testDAO.getTestById(String.valueOf(ref.getItemId()));
                    if (test != null) {
                        PanelItem panelItem = new PanelItem();
                        panelItem.setPanel(panel);
                        panelItem.setPanelName(panel.getPanelName());
                        panelItem.setTest(test);
                        panelItem.setTestName(test.getTestName());
                        panelItem.setSortOrder(String.valueOf(sortOrder++));
                        panelItem.setSysUserId(sysUserId);
                        panelItemDAO.insertData(panelItem);
                    }
                }
            }
        }
    }
}
