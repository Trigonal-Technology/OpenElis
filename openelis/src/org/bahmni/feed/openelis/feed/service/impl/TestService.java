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

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

public class TestService {

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
        try {
            String sysUserId = auditingService.getSysUserId();

            if (odooTest.getIsPanel()) {
                syncPanelFromOdoo(odooTest, sysUserId);
                return;
            }

            // Try to find by external reference first
            ExternalReference data = externalReferenceDao.getData(String.valueOf(odooTest.getId()), CATEGORY_TEST);
            Test test = null;

            if (data != null) {
                test = testDAO.getTestById(String.valueOf(data.getItemId()));
            } else {
                // Try to find by name if external reference doesn't exist
                test = testDAO.getTestByName(odooTest.getName());
            }

            if (test == null) {
                if (odooTest.getActive()) {
                    test = new Test();
                    populateTestFromOdoo(test, odooTest, sysUserId);
                    testDAO.insertData(test);
                    // Save external reference
                    ExternalReference ref = new ExternalReference(Long.parseLong(test.getId()),
                            String.valueOf(odooTest.getId()), CATEGORY_TEST);
                    externalReferenceDao.insertData(ref);
                }
            } else {
                populateTestFromOdoo(test, odooTest, sysUserId);
                testDAO.updateData(test);
            }

            // Simple result type handling (Numerical/Text)
            if (test != null && odooTest.getResultType() != null) {
                if (odooTest.getResultType().equalsIgnoreCase("numerical") ||
                        odooTest.getResultType().equalsIgnoreCase("text")) {
                    testResultService.createOrUpdate(test, "R", null);
                }
            }

            TypeOfSampleUtil.clearTestCache();
        } catch (Exception e) {
            throw new LIMSException(String.format("Error while saving test from Odoo - %s", odooTest.getName()), e);
        }
    }

    private Test populateTestFromOdoo(Test test, OdooTest odooTest, String sysUserId) throws IOException {
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

        // Assign to department (Test Section) if provided - CREATE if it doesn't exist
        if (odooTest.getDepartment() != null && !odooTest.getDepartment().isEmpty()) {
            TestSection section = testSectionDAO.getTestSectionByName(odooTest.getDepartment());
            if (section == null) {
                section = new TestSection();
                section.setTestSectionName(odooTest.getDepartment());
                section.setIsActive(IActionConstants.YES);
                section.setLastupdated(new Timestamp(new Date().getTime()));
                section.setSysUserId(sysUserId);
                testSectionDAO.insertData(section);
            }
            test.setTestSection(section);
        }

        // Assign to default test section if not set
        if (test.getTestSection() == null) {
            test.setTestSection(getTestSection(null));
        }

        // Set Unit of Measure
        if (odooTest.getUom() != null && !odooTest.getUom().isEmpty()) {
            test.setUnitOfMeasure(unitOfMeasureService.create(odooTest.getUom()));
        }

        // Link to Sample Type so it appears in Collect Sample section
        if (odooTest.getSampleType() != null && !odooTest.getSampleType().isEmpty()) {
            linkTestToSampleType(test, odooTest.getSampleType(), sysUserId);
        }

        return test;
    }

    private void linkTestToSampleType(Test test, String sampleTypeName, String sysUserId) {
        try {
            TypeOfSample tosParam = new TypeOfSample();
            tosParam.setDescription(sampleTypeName);
            // Search ignoring case
            TypeOfSample tos = typeOfSampleDAO.getTypeOfSampleByDescriptionAndDomain(tosParam, true);
            if (tos != null) {
                List<TypeOfSampleTest> existingLinks = typeOfSampleTestDAO.getTypeOfSampleTestsForTest(test.getId());
                boolean found = false;
                for (TypeOfSampleTest link : existingLinks) {
                    if (link.getTypeOfSampleId().equals(tos.getId())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    TypeOfSampleTest newLink = new TypeOfSampleTest();
                    newLink.setTestId(test.getId());
                    newLink.setTypeOfSampleId(tos.getId());
                    newLink.setSysUserId(sysUserId);
                    typeOfSampleTestDAO.insertData(newLink);
                }
            }
        } catch (Exception e) {
            // Silently log or handle linking error
        }
    }

    private void syncPanelFromOdoo(OdooTest odooTest, String sysUserId) throws LIMSException {
        try {
            ExternalReference data = externalReferenceDao.getData(String.valueOf(odooTest.getId()), CATEGORY_PANEL);
            Panel panel = null;
            if (data != null) {
                panel = panelDAO.getPanelById(String.valueOf(data.getItemId()));
            } else {
                panel = panelDAO.getPanelByName(odooTest.getName());
            }

            if (panel == null) {
                if (odooTest.getActive()) {
                    panel = new Panel();
                    populatePanelFromOdoo(panel, odooTest, sysUserId);
                    panelDAO.insertData(panel);
                    ExternalReference ref = new ExternalReference(Long.parseLong(panel.getId()),
                            String.valueOf(odooTest.getId()), CATEGORY_PANEL);
                    externalReferenceDao.insertData(ref);
                }
            } else {
                populatePanelFromOdoo(panel, odooTest, sysUserId);
                panelDAO.updateData(panel);
            }

            if (panel != null) {
                syncPanelItems(panel, odooTest, sysUserId);
            }
        } catch (Exception e) {
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
