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
import org.bahmni.feed.openelis.feed.contract.odoo.OdooPatient;
import org.bahmni.feed.openelis.utils.AuditingService;
import us.mn.state.health.lims.common.exception.LIMSRuntimeException;
import us.mn.state.health.lims.hibernate.ElisHibernateSession;
import us.mn.state.health.lims.hibernate.HibernateUtil;
import us.mn.state.health.lims.patient.dao.PatientDAO;
import us.mn.state.health.lims.patient.daoimpl.PatientDAOImpl;
import us.mn.state.health.lims.patient.valueholder.Patient;
import us.mn.state.health.lims.patientidentity.dao.PatientIdentityDAO;
import us.mn.state.health.lims.patientidentity.daoimpl.PatientIdentityDAOImpl;
import us.mn.state.health.lims.patientidentity.valueholder.PatientIdentity;
import us.mn.state.health.lims.patientidentitytype.util.PatientIdentityTypeMap;
import us.mn.state.health.lims.person.dao.PersonDAO;
import us.mn.state.health.lims.person.daoimpl.PersonDAOImpl;
import us.mn.state.health.lims.person.valueholder.Person;

import us.mn.state.health.lims.address.dao.AddressPartDAO;
import us.mn.state.health.lims.address.dao.PersonAddressDAO;
import us.mn.state.health.lims.address.daoimpl.AddressPartDAOImpl;
import us.mn.state.health.lims.address.daoimpl.PersonAddressDAOImpl;
import us.mn.state.health.lims.address.valueholder.AddressPart;
import us.mn.state.health.lims.address.valueholder.AddressParts;
import us.mn.state.health.lims.address.valueholder.PersonAddress;
import us.mn.state.health.lims.address.valueholder.PersonAddresses;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

public class OdooPatientService {
    private static final Logger logger = LogManager.getLogger(OdooPatientService.class);

    private PatientDAO patientDAO;
    private PersonDAO personDAO;
    private PatientIdentityDAO patientIdentityDAO;
    private PersonAddressDAO personAddressDAO;
    private AddressPartDAO addressPartDAO;
    private AuditingService auditingService;

    public OdooPatientService() {
        this(new PatientDAOImpl(), new PersonDAOImpl(), new PatientIdentityDAOImpl(),
                new PersonAddressDAOImpl(), new AddressPartDAOImpl(),
                new AuditingService(new us.mn.state.health.lims.login.daoimpl.LoginDAOImpl(),
                        new us.mn.state.health.lims.siteinformation.daoimpl.SiteInformationDAOImpl()));
    }

    public OdooPatientService(PatientDAO patientDAO, PersonDAO personDAO,
            PatientIdentityDAO patientIdentityDAO,
            PersonAddressDAO personAddressDAO,
            AddressPartDAO addressPartDAO,
            AuditingService auditingService) {
        this.patientDAO = patientDAO;
        this.personDAO = personDAO;
        this.patientIdentityDAO = patientIdentityDAO;
        this.personAddressDAO = personAddressDAO;
        this.addressPartDAO = addressPartDAO;
        this.auditingService = auditingService;
    }

    public void createOrUpdatePatient(OdooPatient odooPatient) {
        String sysUserId = auditingService.getSysUserId();

        try {
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
                logger.info("Created new patient in OpenELIS: ref={}, name={}", odooPatient.getRef(),
                        odooPatient.getName());
            } else {
                // Update patient if UUID matches or needs update
                updatePatient(patient, odooPatient, sysUserId);
                logger.info("Updated existing patient in OpenELIS: ref={}, name={}", odooPatient.getRef(),
                        odooPatient.getName());
            }

        } catch (Exception e) {
            logger.error("Error processing Odoo patient: " + odooPatient.getRef(), e);
            ElisHibernateSession session = (ElisHibernateSession) us.mn.state.health.lims.hibernate.HibernateUtil
                    .getSession();
            session.clearSession();
            throw new LIMSRuntimeException("Error processing patient: " + e.getMessage(), e);
        }
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
            addOrUpdateIdentity(patient.getId(), "ST", odooPatient.getRef(), sysUserId);
        }

        // Create primary relative identity (PR)
        if (odooPatient.getPrimaryRelative() != null && !odooPatient.getPrimaryRelative().isEmpty()) {
            addOrUpdateIdentity(patient.getId(), "PR", odooPatient.getPrimaryRelative(), sysUserId);
        }

        // Create occupation identity (OC)
        if (odooPatient.getOccupation() != null && !odooPatient.getOccupation().isEmpty()) {
            addOrUpdateIdentity(patient.getId(), "OC", odooPatient.getOccupation(), sysUserId);
        }

        // Set address if provided
        if (odooPatient.getAddress() != null && !odooPatient.getAddress().isEmpty()) {
            createOrUpdateAddress(person, odooPatient.getAddress(), sysUserId);
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

        // Update primary relative identity (PR)
        if (odooPatient.getPrimaryRelative() != null && !odooPatient.getPrimaryRelative().isEmpty()) {
            addOrUpdateIdentity(patient.getId(), "PR", odooPatient.getPrimaryRelative(), sysUserId);
        }

        // Update occupation identity (OC)
        if (odooPatient.getOccupation() != null && !odooPatient.getOccupation().isEmpty()) {
            addOrUpdateIdentity(patient.getId(), "OC", odooPatient.getOccupation(), sysUserId);
        }

        // Update address if provided
        if (odooPatient.getAddress() != null && !odooPatient.getAddress().isEmpty()) {
            createOrUpdateAddress(patient.getPerson(), odooPatient.getAddress(), sysUserId);
        }
    }

    private void addOrUpdateIdentity(String patientId, String typeCode, String value, String sysUserId) {
        String typeId = PatientIdentityTypeMap.getInstance().getIDForType(typeCode);
        if (typeId == null) {
            logger.warn("Identity type code '{}' not found in OpenELIS", typeCode);
            return;
        }

        List<PatientIdentity> identities = patientIdentityDAO.getPatientIdentitiesForPatient(patientId);
        PatientIdentity identity = null;
        for (PatientIdentity i : identities) {
            if (typeId.equals(i.getIdentityTypeId())) {
                identity = i;
                break;
            }
        }

        if (identity == null) {
            identity = new PatientIdentity();
            identity.setPatientId(patientId);
            identity.setIdentityTypeId(typeId);
            identity.setIdentityData(value);
            identity.setSysUserId(sysUserId);
            patientIdentityDAO.insertData(identity);
        } else if (!value.equals(identity.getIdentityData())) {
            identity.setIdentityData(value);
            identity.setSysUserId(sysUserId);
            patientIdentityDAO.updateData(identity);
        }
    }

    private void createOrUpdateAddress(Person person, Map<String, String> addressMap, String sysUserId) {
        AddressParts addressParts = new AddressParts(addressPartDAO.getAll());
        PersonAddresses personAddresses = new PersonAddresses(
                personAddressDAO.getAddressPartsByPersonId(person.getId()), addressParts);

        if (addressMap.get("street") != null) {
            insertOrUpdateAddressPart(person, addressParts, personAddresses, "level1", addressMap.get("street"),
                    sysUserId);
        }
        if (addressMap.get("city") != null) {
            insertOrUpdateAddressPart(person, addressParts, personAddresses, "level2", addressMap.get("city"),
                    sysUserId);
        }
        if (addressMap.get("street2") != null) {
            insertOrUpdateAddressPart(person, addressParts, personAddresses, "level3", addressMap.get("street2"),
                    sysUserId);
        }
        if (addressMap.get("zip") != null) {
            insertOrUpdateAddressPart(person, addressParts, personAddresses, "level4", addressMap.get("zip"),
                    sysUserId);
        }
        if (addressMap.get("state") != null) {
            insertOrUpdateAddressPart(person, addressParts, personAddresses, "level6", addressMap.get("state"),
                    sysUserId);
        }
    }

    private void insertOrUpdateAddressPart(Person person, AddressParts addressParts, PersonAddresses personAddresses,
            String partName, String value, String sysUserId) {
        PersonAddress addressPart = personAddresses.findByPartName(partName);
        if (addressPart != null) {
            addressPart.updateValue(value, sysUserId);
            personAddressDAO.update(addressPart);
        } else {
            PersonAddress newPart = PersonAddress.create(person, addressParts, partName, value, sysUserId);
            personAddressDAO.insert(newPart);
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
}
