package com.projet.api_validation.service;

import com.helger.schematron.ISchematronResource;
import com.helger.schematron.sch.SchematronResourceSCH;
import com.helger.schematron.svrl.SVRLHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ValidationService {

    // -------------------------------------------------------------------------
    // Classe interne métier pour encapsuler l'état et le résultat de validation
    // -------------------------------------------------------------------------
    private static class ValidationReport {
        private boolean valid = true;
        private final List<String> messages = new ArrayList<>();

        public boolean isValid() {
            return valid;
        }

        public List<String> getMessages() {
            return messages;
        }

        public void addError(String errorMessage) {
            this.valid = false;
            this.messages.add("❌ " + errorMessage);
        }

        public void addSuccess(String successMessage) {
            this.messages.add("✅ " + successMessage);
        }

        public void addDetail(String detailMessage) {
            this.messages.add(detailMessage);
        }

        public void merge(ValidationReport other) {
            this.valid = this.valid && other.valid;
            this.messages.addAll(other.messages);
        }
    }

    // -------------------------------------------------------------------------
    // Méthode publique principale
    // -------------------------------------------------------------------------
    public List<String> validerFactureUBL(MultipartFile file, String format) throws Exception {
        byte[] xmlBytes = file.getBytes();
        ValidationReport globalReport = new ValidationReport();

        // Étape 1 : Validation structurelle XSD
        ValidationReport xsdReport = validerFactureXSD(xmlBytes);
        globalReport.merge(xsdReport);

        if (xsdReport.isValid()) {
            // Étape 2 : Validation des règles métier (Schematron)
            ValidationReport schematronReport = validerFactureSchematrons(xmlBytes, format);
            globalReport.merge(schematronReport);
        }

        return globalReport.getMessages();
    }

    // -------------------------------------------------------------------------
    // Méthode privée : Validation de structure (XSD)
    // -------------------------------------------------------------------------
    private ValidationReport validerFactureXSD(byte[] xmlBytes) {
        ValidationReport report = new ValidationReport();

        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            URL xsdUrl = getClass().getClassLoader().getResource("schemas/xsd/maindoc/UBL-Invoice-2.1.xsd");

            if (xsdUrl == null) {
                report.addError("Fichier de configuration XSD principal introuvable");
                return report;
            }

            Schema schema = factory.newSchema(xsdUrl);
            Validator validator = schema.newValidator();

            List<String> xsdErreurs = new ArrayList<>();
            validator.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) {
                }

                @Override
                public void error(SAXParseException exception) {
                    xsdErreurs.add("- Ligne " + exception.getLineNumber() + " : " + exception.getMessage());
                }

                @Override
                public void fatalError(SAXParseException exception) {
                    xsdErreurs.add("- Ligne " + exception.getLineNumber() + " (Critique) : " + exception.getMessage());
                }
            });

            try {
                validator.validate(new StreamSource(new ByteArrayInputStream(xmlBytes)));
            } catch (Exception e) {
                // Erreur si grave que le parsing XML plante avant même l'ErrorHandler
                if (xsdErreurs.isEmpty()) {
                    report.addError(
                            "Erreur fatale de structuration XML (Impossible de lire le fichier) : " + e.getMessage());
                    return report;
                }
            }

            if (!xsdErreurs.isEmpty()) {
                report.addError(xsdErreurs.size() + " erreur(s) de structure (XSD) trouvées :");
                xsdErreurs.forEach(report::addDetail);
            } else {
                report.addSuccess("Validation XSD réussie (Format UBL Invoice 2.1 valide).");
            }

        } catch (Exception e) {
            report.addError("Erreur interne critique lors du test XSD : " + e.getMessage());
        }

        return report;
    }

    // -------------------------------------------------------------------------
    // Méthode privée : Validation des règles métier (Schematron)
    // -------------------------------------------------------------------------
    private ValidationReport validerFactureSchematrons(byte[] xmlBytes, String format) {
        ValidationReport report = new ValidationReport();

        try {
            Map<String, ISchematronResource> schematrons = new LinkedHashMap<>();

            if ("EN16931".equals(format)) {
                schematrons.put("EN16931", SchematronResourceSCH
                        .fromClassPath("schemas/schematrons/EN16931-UBL-validation-preprocessed.sch"));
            } else if ("EXTENDED".equals(format)) {
                schematrons.put("EXTENDED", SchematronResourceSCH
                        .fromClassPath("schemas/schematrons/20260216_EXTENDED-CTC-FR-UBL-V1.3.0.sch"));
                schematrons.put("BR-FR", SchematronResourceSCH
                        .fromClassPath("schemas/schematrons/20260216_BR-FR-Flux2-Schematron-UBL_V1.3.0.sch"));
            } else {
                report.addError("Le format de la facture n'est pas reconnu : " + format);
                return report;
            }

            for (Map.Entry<String, ISchematronResource> entry : schematrons.entrySet()) {
                String label = entry.getKey();
                ISchematronResource schematron = entry.getValue();

                if (!schematron.isValidSchematron()) {
                    report.addError(
                            "Le fichier Schematron spécifié pour \"" + label + "\" est introuvable ou mal formatté.");
                    continue; // On passe au schematron suivant s'il y en a un
                }

                var schResult = schematron
                        .applySchematronValidationToSVRL(new StreamSource(new ByteArrayInputStream(xmlBytes)));

                if (schResult == null) {
                    report.addError(
                            "Erreur interne inattendue lors de l'application du fichier Schematron \"" + label + "\".");
                    return report;
                }

                var failedAsserts = SVRLHelper.getAllFailedAssertions(schResult);

                if (failedAsserts.isEmpty()) {
                    report.addSuccess("Validation métier Schematrons (" + label + ") réussie : Aucune erreur !");
                } else {
                    long nbFatal = failedAsserts.stream()
                            .filter(fa -> fa.getFlag() != null && "fatal".equalsIgnoreCase(fa.getFlag().getID()))
                            .count();
                    long nbWarning = failedAsserts.size() - nbFatal;

                    String resume = "Échec Schematrons (" + label + ") : "
                            + (nbFatal > 0 ? nbFatal + " erreur(s) fatale(s)" : "")
                            + (nbFatal > 0 && nbWarning > 0 ? ", " : "")
                            + (nbWarning > 0 ? nbWarning + " avertissement(s)" : "");

                    report.addError(resume);

                    for (var fa : failedAsserts) {
                        boolean isFatal = fa.getFlag() != null && "fatal".equalsIgnoreCase(fa.getFlag().getID());
                        String prefix = isFatal ? "🔴 [FATAL] " : "🟡 [AVERTISSEMENT] ";
                        String id = fa.getID() != null ? "[" + fa.getID() + "] " : "";
                        String location = fa.getLocation() != null ? " (emplacement : " + fa.getLocation() + ")" : "";
                        String message = fa.getText() != null ? fa.getText() : "(pas de message)";

                        report.addDetail("  • " + prefix + id + message + location);
                    }
                }
            }

        } catch (Exception e) {
            report.addError("Erreur technique pendant le traitement Schematron : " + e.getMessage());
        }

        return report;
    }
}
