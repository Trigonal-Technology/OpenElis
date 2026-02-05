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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bahmni.feed.openelis.externalreference.dao.ExternalReferenceDao;
import org.bahmni.feed.openelis.externalreference.daoimpl.ExternalReferenceDaoImpl;
import org.bahmni.feed.openelis.externalreference.valueholder.ExternalReference;
import org.bahmni.feed.openelis.feed.contract.SampleTestOrderCollection;
import org.bahmni.feed.openelis.feed.contract.TestOrder;
import org.bahmni.feed.openelis.feed.contract.odoo.OdooPatient;
import org.bahmni.feed.openelis.feed.contract.odoo.OdooTestOrder;
import org.bahmni.feed.openelis.feed.contract.odoo.OdooTestOrderLine;
import org.bahmni.feed.openelis.utils.AuditingService;
import us.mn.state.health.lims.address.valueholder.OrganizationAddress;
import us.mn.state.health.lims.analysis.dao.AnalysisDAO;
import us.mn.state.health.lims.analysis.daoimpl.AnalysisDAOImpl;
import us.mn.state.health.lims.analysis.valueholder.Analysis;
import us.mn.state.health.lims.common.exception.LIMSRuntimeException;
import us.mn.state.health.lims.common.util.DateUtil;
import us.mn.state.health.lims.common.util.SystemConfiguration;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import us.mn.state.health.lims.hibernate.ElisHibernateSession;
import us.mn.state.health.lims.hibernate.HibernateUtil;
import us.mn.state.health.lims.login.daoimpl.LoginDAOImpl;
import us.mn.state.health.lims.observationhistory.valueholder.ObservationHistory;
import us.mn.state.health.lims.organization.dao.OrganizationTypeDAO;
import us.mn.state.health.lims.organization.daoimpl.OrganizationTypeDAOImpl;
import us.mn.state.health.lims.panelitem.dao.PanelItemDAO;
import us.mn.state.health.lims.panelitem.daoimpl.PanelItemDAOImpl;
import us.mn.state.health.lims.panelitem.valueholder.PanelItem;
import us.mn.state.health.lims.patient.dao.PatientDAO;
import us.mn.state.health.lims.patient.daoimpl.PatientDAOImpl;
import us.mn.state.health.lims.patient.valueholder.Patient;
import us.mn.state.health.lims.patientidentity.dao.PatientIdentityDAO;
import us.mn.state.health.lims.patientidentity.daoimpl.PatientIdentityDAOImpl;
import us.mn.state.health.lims.patientidentity.valueholder.PatientIdentity;
import us.mn.state.health.lims.patientidentitytype.dao.PatientIdentityTypeDAO;
import us.mn.state.health.lims.patientidentitytype.daoimpl.PatientIdentityTypeDAOImpl;
import us.mn.state.health.lims.patientidentitytype.util.PatientIdentityTypeMap;
import us.mn.state.health.lims.person.dao.PersonDAO;
import us.mn.state.health.lims.person.daoimpl.PersonDAOImpl;
import us.mn.state.health.lims.person.valueholder.Person;
import us.mn.state.health.lims.provider.dao.ProviderDAO;
import us.mn.state.health.lims.provider.daoimpl.ProviderDAOImpl;
import us.mn.state.health.lims.requester.dao.RequesterTypeDAO;
import us.mn.state.health.lims.requester.daoimpl.RequesterTypeDAOImpl;
import us.mn.state.health.lims.requester.valueholder.RequesterType;
import us.mn.state.health.lims.sample.dao.SampleDAO;
import us.mn.state.health.lims.sample.daoimpl.SampleDAOImpl;
import us.mn.state.health.lims.sample.util.AnalysisBuilder;
import us.mn.state.health.lims.sample.valueholder.Sample;
import us.mn.state.health.lims.samplehuman.valueholder.SampleHuman;
import us.mn.state.health.lims.sampleitem.dao.SampleItemDAO;
import us.mn.state.health.lims.sampleitem.daoimpl.SampleItemDAOImpl;
import us.mn.state.health.lims.sampleitem.valueholder.SampleItem;
import us.mn.state.health.lims.samplesource.dao.SampleSourceDAO;
import us.mn.state.health.lims.samplesource.daoimpl.SampleSourceDAOImpl;
import us.mn.state.health.lims.samplesource.valueholder.SampleSource;
import us.mn.state.health.lims.siteinformation.daoimpl.SiteInformationDAOImpl;
import us.mn.state.health.lims.statusofsample.util.StatusOfSampleUtil;
import us.mn.state.health.lims.test.dao.TestDAO;
import us.mn.state.health.lims.test.daoimpl.TestDAOImpl;
import us.mn.state.health.lims.test.valueholder.Test;
import us.mn.state.health.lims.typeofsample.dao.TypeOfSampleDAO;
import us.mn.state.health.lims.typeofsample.dao.TypeOfSampleTestDAO;
import us.mn.state.health.lims.typeofsample.daoimpl.TypeOfSampleDAOImpl;
import us.mn.state.health.lims.typeofsample.daoimpl.TypeOfSampleTestDAOImpl;
import us.mn.state.health.lims.typeofsample.util.TypeOfSampleUtil;
import us.mn.state.health.lims.typeofsample.valueholder.TypeOfSample;
import us.mn.state.health.lims.upload.action.AddSampleService;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OdooTestOrderService {
    private static final Logger logger = LogManager.getLogger(OdooTestOrderService.class);

    private PatientDAO patientDAO;
    private PersonDAO personDAO;
    private PatientIdentityDAO patientIdentityDAO;
    private PatientIdentityTypeDAO patientIdentityTypeDAO;
    private SampleDAO sampleDAO;
    private ExternalReferenceDao externalReferenceDao;
    private PanelItemDAO panelItemDAO;
    private TypeOfSampleTestDAO typeOfSampleTestDAO;
    private TypeOfSampleDAO typeOfSampleDAO;
    private TestDAO testDAO;
    private AnalysisDAO analysisDAO;
    private SampleItemDAO sampleItemDAO;
    private SampleSourceDAO sampleSourceDAO;
    private AuditingService auditingService;
    private RequesterTypeDAO requesterTypeDAO;
    private OrganizationTypeDAO organizationTypeDAO;
    private ProviderDAO providerDAO;

    public OdooTestOrderService() {
        this(new PatientDAOImpl(), new PersonDAOImpl(), new PatientIdentityDAOImpl(),
                new PatientIdentityTypeDAOImpl(), new SampleDAOImpl(), new ExternalReferenceDaoImpl(),
                new PanelItemDAOImpl(), new TypeOfSampleTestDAOImpl(), new TypeOfSampleDAOImpl(),
                new TestDAOImpl(), new AnalysisDAOImpl(), new SampleItemDAOImpl(),
                new SampleSourceDAOImpl(), new AuditingService(new LoginDAOImpl(), new SiteInformationDAOImpl()),
                new RequesterTypeDAOImpl(), new OrganizationTypeDAOImpl(), new ProviderDAOImpl());
    }

    public OdooTestOrderService(PatientDAO patientDAO, PersonDAO personDAO, PatientIdentityDAO patientIdentityDAO,
            PatientIdentityTypeDAO patientIdentityTypeDAO, SampleDAO sampleDAO,
            ExternalReferenceDao externalReferenceDao, PanelItemDAO panelItemDAO,
            TypeOfSampleTestDAO typeOfSampleTestDAO, TypeOfSampleDAO typeOfSampleDAO,
            TestDAO testDAO, AnalysisDAO analysisDAO, SampleItemDAO sampleItemDAO,
            SampleSourceDAO sampleSourceDAO, AuditingService auditingService,
            RequesterTypeDAO requesterTypeDAO, OrganizationTypeDAO organizationTypeDAO,
            ProviderDAO providerDAO) {
        this.patientDAO = patientDAO;
        this.personDAO = personDAO;
        this.patientIdentityDAO = patientIdentityDAO;
        this.patientIdentityTypeDAO = patientIdentityTypeDAO;
        this.sampleDAO = sampleDAO;
        this.externalReferenceDao = externalReferenceDao;
        this.panelItemDAO = panelItemDAO;
        this.typeOfSampleTestDAO = typeOfSampleTestDAO;
        this.testDAO = testDAO;
        this.analysisDAO = analysisDAO;
        this.sampleItemDAO = sampleItemDAO;
        this.sampleSourceDAO = sampleSourceDAO;
        this.auditingService = auditingService;
        this.requesterTypeDAO = requesterTypeDAO;
        this.organizationTypeDAO = organizationTypeDAO;
        this.providerDAO = providerDAO;
        this.typeOfSampleTestDAO = typeOfSampleTestDAO;
        this.typeOfSampleDAO = typeOfSampleDAO;
    }

    public void processTestOrder(OdooTestOrder odooTestOrder) {
        String sysUserId = auditingService.getSysUserId();

        try {
            // Validate and get/create patient
            Patient patient = getOrCreatePatient(odooTestOrder.getPatient(), sysUserId);

            // Check if sample already exists for this sale order (using UUID)
            List<Sample> existingSamples = sampleDAO.getSamplesByEncounterUuid(odooTestOrder.getSaleOrderId());
            if (existingSamples != null && !existingSamples.isEmpty()) {
                logger.warn("Sample already exists for sale order ID: " + odooTestOrder.getSaleOrderId());
                throw new LIMSRuntimeException(
                        "Sample already exists for sale order ID: " + odooTestOrder.getSaleOrderId());
            }

            // Create sample
            Date nowAsSqlDate = DateUtil.getNowAsSqlDate();
            Sample sample = createSample(odooTestOrder, sysUserId, nowAsSqlDate);

            // Create sample test order collections
            List<SampleTestOrderCollection> sampleTestOrderCollectionList = createSampleTestOrderCollections(
                    odooTestOrder.getOrderLines(), sysUserId, nowAsSqlDate, sample);

            if (sampleTestOrderCollectionList.isEmpty()) {
                logger.warn("No valid test orders found for sale order: " + odooTestOrder.getSaleOrderId());
                throw new LIMSRuntimeException("No valid test orders found");
            }

            // Create sample human link
            SampleHuman sampleHuman = new SampleHuman();
            sampleHuman.setSysUserId(sysUserId);

            // Create analysis builder
            AnalysisBuilder analysisBuilder = new AnalysisBuilder();

            // Persist sample
            AddSampleService addSampleService = new AddSampleService(false);
            long providerRequesterTypeId = getProviderRequesterTypeId();
            String referringOrgTypeId = getReferringOrgTypeId();
            addSampleService.persist(analysisBuilder, false, null, null,
                    new ArrayList<OrganizationAddress>(), sample,
                    sampleTestOrderCollectionList, new ArrayList<ObservationHistory>(), sampleHuman,
                    patient.getId(), null, null, sysUserId,
                    providerRequesterTypeId, referringOrgTypeId);

            logger.info("Successfully created sample for sale order: " + odooTestOrder.getSaleOrderId());

        } catch (Exception e) {
            logger.error("Error processing Odoo test order: " + odooTestOrder.getSaleOrderId(), e);
            ElisHibernateSession session = (ElisHibernateSession) HibernateUtil.getSession();
            session.clearSession();
            throw new LIMSRuntimeException("Error processing test order: " + e.getMessage(), e);
        }
    }

    private Patient getOrCreatePatient(OdooPatient odooPatient, String sysUserId) {
        // First try to find by UUID
        Patient patient = null;
        if (odooPatient.getUuid() != null && !odooPatient.getUuid().isEmpty()) {
            patient = patientDAO.getPatientByUUID(odooPatient.getUuid());
        }

        // If not found by UUID, try to find by ref (ST identifier)
        if (patient == null && odooPatient.getRef() != null && !odooPatient.getRef().isEmpty()) {
            String stTypeId = PatientIdentityTypeMap.getInstance().getIDForType("ST");
            List<Patient> patients = patientDAO.getPatientsByPatientIdentityValue(stTypeId, odooPatient.getRef());
            if (patients != null && !patients.isEmpty()) {
                patient = patients.get(0);
            }
        }

        // If still not found, create new patient
        if (patient == null) {
            patient = createPatient(odooPatient, sysUserId);
        } else {
            // Update patient if UUID matches or needs update
            updatePatient(patient, odooPatient, sysUserId);
        }

        return patient;
    }

    private Patient createPatient(OdooPatient odooPatient, String sysUserId) {
        // Create person
        Person person = new Person();
        String[] nameParts = parseName(odooPatient.getName());
        person.setFirstName(nameParts[0]);
        person.setMiddleName(nameParts.length > 2 ? nameParts[1] : null);
        person.setLastName(nameParts.length > 1 ? nameParts[nameParts.length - 1] : null);
        person.setSysUserId(sysUserId);
        personDAO.insertData(person);

        // Create patient
        Patient patient = new Patient();
        patient.setPerson(person);
        patient.setSysUserId(sysUserId);
        if (odooPatient.getUuid() != null && !odooPatient.getUuid().isEmpty()) {
            patient.setUuid(odooPatient.getUuid());
        }

        // Set birthdate if provided
        if (odooPatient.getBirthdate() != null && !odooPatient.getBirthdate().isEmpty()) {
            try {
                // Parse ISO date string (YYYY-MM-DD) to Timestamp
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd");
                java.util.Date birthDate = isoFormat.parse(odooPatient.getBirthdate());
                patient.setBirthDate(new Timestamp(birthDate.getTime()));
            } catch (ParseException e) {
                logger.warn("Failed to parse birthdate '{}' for patient {}. Error: {}",
                        odooPatient.getBirthdate(), odooPatient.getRef(), e.getMessage());
            }
        }

        // Set gender if provided (convert Odoo values to OpenELIS format: M, F, or
        // empty)
        if (odooPatient.getGender() != null && !odooPatient.getGender().isEmpty()) {
            String genderCode = "";
            if (odooPatient.getGender().equalsIgnoreCase("male")) {
                genderCode = "M";
            } else if (odooPatient.getGender().equalsIgnoreCase("female")) {
                genderCode = "F";
            } else if (odooPatient.getGender().equalsIgnoreCase("other")) {
                // OpenELIS may not support "other", use empty or handle as needed
                genderCode = "";
            }
            if (!genderCode.isEmpty()) {
                patient.setGender(genderCode);
            }
        }

        patientDAO.insertData(patient);

        // Create patient identity (ST - ref field)
        if (odooPatient.getRef() != null && !odooPatient.getRef().isEmpty()) {
            String stTypeId = PatientIdentityTypeMap.getInstance().getIDForType("ST");
            PatientIdentity patientIdentity = new PatientIdentity();
            patientIdentity.setIdentityTypeId(stTypeId);
            patientIdentity.setPatientId(patient.getId());
            patientIdentity.setIdentityData(odooPatient.getRef());
            patientIdentity.setSysUserId(sysUserId);
            patientIdentityDAO.insertData(patientIdentity);
        }

        return patient;
    }

    private void updatePatient(Patient patient, OdooPatient odooPatient, String sysUserId) {
        // Update UUID if provided and different
        if (odooPatient.getUuid() != null && !odooPatient.getUuid().isEmpty()
                && (patient.getUuid() == null || !patient.getUuid().equals(odooPatient.getUuid()))) {
            patient.setUuid(odooPatient.getUuid());
            patient.setSysUserId(sysUserId);
            patientDAO.updateData(patient);
        }

        // Update person name if provided
        Person person = patient.getPerson();
        if (odooPatient.getName() != null && !odooPatient.getName().isEmpty()) {
            String[] nameParts = parseName(odooPatient.getName());
            person.setFirstName(nameParts[0]);
            person.setMiddleName(nameParts.length > 2 ? nameParts[1] : null);
            person.setLastName(nameParts.length > 1 ? nameParts[nameParts.length - 1] : null);
            person.setSysUserId(sysUserId);
            personDAO.updateData(person);
        }

        // Update birthdate if provided
        if (odooPatient.getBirthdate() != null && !odooPatient.getBirthdate().isEmpty()) {
            try {
                // Parse ISO date string (YYYY-MM-DD) to Timestamp
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd");
                java.util.Date birthDate = isoFormat.parse(odooPatient.getBirthdate());
                Timestamp birthTimestamp = new Timestamp(birthDate.getTime());

                // Only update if different
                if (patient.getBirthDate() == null || !patient.getBirthDate().equals(birthTimestamp)) {
                    patient.setBirthDate(birthTimestamp);
                    patient.setSysUserId(sysUserId);
                    patientDAO.updateData(patient);
                }
            } catch (ParseException e) {
                logger.warn("Failed to parse birthdate '{}' for patient {}. Error: {}",
                        odooPatient.getBirthdate(), odooPatient.getRef(), e.getMessage());
            }
        }

        // Update gender if provided
        if (odooPatient.getGender() != null && !odooPatient.getGender().isEmpty()) {
            String genderCode = "";
            if (odooPatient.getGender().equalsIgnoreCase("male")) {
                genderCode = "M";
            } else if (odooPatient.getGender().equalsIgnoreCase("female")) {
                genderCode = "F";
            } else if (odooPatient.getGender().equalsIgnoreCase("other")) {
                genderCode = "";
            }
            if (!genderCode.isEmpty() && (patient.getGender() == null || !patient.getGender().equals(genderCode))) {
                patient.setGender(genderCode);
                patient.setSysUserId(sysUserId);
                patientDAO.updateData(patient);
            }
        }
    }

    private String[] parseName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return new String[] { "Unknown", "" };
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return new String[] { parts[0], "" };
        }
        return parts;
    }

    private Sample createSample(OdooTestOrder odooTestOrder, String sysUserId, Date nowAsSqlDate) {
        Sample sample = new Sample();
        sample.setSysUserId(sysUserId);
        sample.setAccessionNumber(null);

        // Set sample source - use default or first available
        List<SampleSource> sampleSources = sampleSourceDAO.getAll();
        if (sampleSources != null && !sampleSources.isEmpty()) {
            sample.setSampleSource(sampleSources.get(0));
        }

        sample.setEnteredDate(new java.util.Date());
        sample.setReceivedDate(nowAsSqlDate);
        sample.setDomain(SystemConfiguration.getInstance().getHumanDomain());
        sample.setStatusId(StatusOfSampleUtil.getStatusID(StatusOfSampleUtil.OrderStatus.Entered));

        // Use sale order ID as UUID
        sample.setUUID(odooTestOrder.getSaleOrderId());

        return sample;
    }

    private List<SampleTestOrderCollection> createSampleTestOrderCollections(
            List<OdooTestOrderLine> orderLines, String sysUserId, Date nowAsSqlDate, Sample sample) {

        List<SampleTestOrderCollection> sampleTestOrderCollectionList = new ArrayList<>();
        Map<String, SampleItem> sampleItemMap = new HashMap<>();
        Map<String, List<TestOrder>> testOrderMap = new HashMap<>();

        for (OdooTestOrderLine orderLine : orderLines) {
            try {
                List<TestOrder> testOrders = getTestsForOrderLine(orderLine);

                for (TestOrder testOrder : testOrders) {
                    TypeOfSample typeOfSample = TypeOfSampleUtil.getTypeOfSampleForTest(testOrder.getTest().getId());
                    if (typeOfSample == null) {
                        String testName = testOrder.getTest().getTestName();
                        String testId = testOrder.getTest().getId();
                        logger.error("Test '{}' (ID: {}) is not linked to any sample type.", testName, testId);
                        throw new LIMSRuntimeException(
                                String.format("Test '%s' (ID: %s) is not linked to any sample type. " +
                                        "Please link this test to a sample type in OpenELIS.",
                                        testName, testId));
                    }
                    String sampleTypeId = typeOfSample.getId();

                    // Get or create sample item for this sample type
                    SampleItem sampleItem = sampleItemMap.get(sampleTypeId);
                    if (sampleItem == null) {
                        sampleItem = buildSampleItem(sysUserId, sample, sampleItemMap.size() + 1, typeOfSample);
                        sampleItemMap.put(sampleTypeId, sampleItem);
                        testOrderMap.put(sampleTypeId, new ArrayList<TestOrder>());
                    }

                    // Add test order to the list for this sample item
                    testOrderMap.get(sampleTypeId).add(testOrder);
                }
            } catch (Exception e) {
                logger.error("Error processing order line for product: " + orderLine.getProductUuid(), e);
                // Continue with other order lines
            }
        }

        // Create SampleTestOrderCollection for each sample item
        for (Map.Entry<String, SampleItem> entry : sampleItemMap.entrySet()) {
            String sampleTypeId = entry.getKey();
            SampleItem sampleItem = entry.getValue();
            List<TestOrder> testOrders = testOrderMap.get(sampleTypeId);

            if (testOrders != null && !testOrders.isEmpty()) {
                SampleTestOrderCollection collection = new SampleTestOrderCollection(sampleItem, testOrders,
                        nowAsSqlDate);
                sampleTestOrderCollectionList.add(collection);
            }
        }
        return sampleTestOrderCollectionList;
    }

    private List<TestOrder> getTestsForOrderLine(OdooTestOrderLine orderLine) {
        String productUuid = orderLine.getProductUuid();
        String productType = orderLine.getProductType();

        if ("Panel".equals(productType)) {
            return getTestsForPanel(productUuid, orderLine.getComment());
        } else {
            return getTest(productUuid, orderLine.getComment());
        }
    }

    private List<TestOrder> getTest(String productUuid, String comment) {
        logger.info("Looking up test with product UUID: {}", productUuid);
        String productTypeTest = "Test";
        ExternalReference externalRef = externalReferenceDao.getData(productUuid, productTypeTest);
        if (externalRef == null) {
            logger.error("No external reference found for product UUID '{}' with type '{}'", productUuid,
                    productTypeTest);
            throw new LIMSRuntimeException(
                    String.format(
                            "Test with UUID '%s' was not setup properly. No external reference found in external_reference table",
                            productUuid));
        }

        long testId = externalRef.getItemId();
        logger.info("Found external reference: product UUID {} -> test ID {}", productUuid, testId);
        Test test = testDAO.getTestById(String.valueOf(testId));
        if (test == null) {
            logger.error("Test with ID {} not found in database", testId);
            throw new LIMSRuntimeException("Test with ID " + testId + " not found");
        }

        logger.info("Successfully retrieved test: {} (ID: {})", test.getTestName(), test.getId());

        List<TestOrder> tests = new ArrayList<>();
        tests.add(new TestOrder(test, comment));
        return tests;
    }

    private List<TestOrder> getTestsForPanel(String productUuid, String comment) {
        List<TestOrder> testOrders = new ArrayList<>();
        String productTypePanel = "Panel";
        ExternalReference externalRef = externalReferenceDao.getData(productUuid, productTypePanel);
        if (externalRef == null) {
            throw new LIMSRuntimeException(
                    String.format(
                            "Panel with UUID '%s' was not setup properly. No external reference found in external_reference table",
                            productUuid));
        }

        long panelId = externalRef.getItemId();
        List panelItemsForPanel = panelItemDAO.getPanelItemsForPanel(String.valueOf(panelId));
        for (Object obj : panelItemsForPanel) {
            PanelItem panelItem = (PanelItem) obj;
            testOrders.add(new TestOrder(panelItem.getTest(), comment));
        }

        return testOrders;
    }

    private SampleItem buildSampleItem(String sysUserId, Sample sample, int sortOrder, TypeOfSample typeOfSample) {
        SampleItem item = new SampleItem();
        item.setSysUserId(sysUserId);
        item.setSample(sample);
        item.setTypeOfSample(typeOfSample);
        item.setSortOrder(Integer.toString(sortOrder));
        item.setStatusId(StatusOfSampleUtil.getStatusID(StatusOfSampleUtil.SampleStatus.Entered));
        item.setCollector("");
        return item;
    }

    private long getProviderRequesterTypeId() {
        try {
            RequesterType requesterType = requesterTypeDAO.getRequesterTypeByName("provider");
            if (requesterType != null && requesterType.getId() != null) {
                return Long.parseLong(requesterType.getId());
            }
            return 0L; // Default to 0 if not found
        } catch (Exception e) {
            logger.warn("Could not find provider requester type", e);
            return 0L;
        }
    }

    private String getReferringOrgTypeId() {
        // Return default or first organization type ID
        // This can be customized based on requirements
        return null;
    }
}
