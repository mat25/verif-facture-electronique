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
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

@Service
public class ValidationService {

    public List<String> validerFactureUBL(MultipartFile file, String format) throws Exception {
        List<String> resultats = new ArrayList<>();
        byte[] xmlBytes = file.getBytes();

        // 1. Validation de Structure (XSD)
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            // On charge le XSD principal UBL 2.1
            java.net.URL xsdUrl = getClass().getClassLoader().getResource("schemas/xsd/maindoc/UBL-Invoice-2.1.xsd");
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

            // On valide le XML avec le XSD
            try {
                validator.validate(new StreamSource(new ByteArrayInputStream(xmlBytes)));
            } catch (Exception e) {
                // Si l'erreur est si grave que le parsing plante avant même de déclencher
                // l'ErrorHandler
                if (xsdErreurs.isEmpty()) {
                    resultats.add("❌ Erreur fatale XSD : " + e.getMessage());
                    return resultats;
                }
            }

            if (!xsdErreurs.isEmpty()) {
                resultats.add("❌ " + xsdErreurs.size() + " erreur(s) de structure (XSD) trouvées :");
                resultats.addAll(xsdErreurs);
                return resultats; // On bloque ici, pas la peine de lancer le Schematron
            } else {
                resultats.add("✅ Validation XSD réussie (Format UBL Invoice 2.1 valide).");
            }
        } catch (Exception e) {
            resultats.add("❌ Erreur interne lors du test XSD : " + e.getMessage());
            return resultats;
        }

        // 2. Validation des regles metiers (Schematron)
        try {
            // Créer un tableau de Schematron
            List<ISchematronResource> schematrons = new ArrayList<>();

            // On ajoute les fichiers Schematron en fonction du format
            if (format.equals("EN16931")) {
                schematrons.add(SchematronResourceSCH
                        .fromClassPath("schemas/schematrons/EN16931-UBL-validation-preprocessed.sch"));

            } else if (format.equals("EXTENDED")) {
                schematrons.add(SchematronResourceSCH
                        .fromClassPath("schemas/schematrons/20260216_EXTENDED-CTC-FR-UBL-V1.3.0.sch"));

                schematrons.add(SchematronResourceSCH
                        .fromClassPath("schemas/schematrons/20260216_BR-FR-Flux2-Schematron-UBL_V1.3.0.sch"));
            } else {
                throw new IllegalStateException("Le format de la facture n'est pas reconnu.");
            }

            for (ISchematronResource schematron : schematrons) {

                if (!schematron.isValidSchematron()) {
                    throw new IllegalStateException("Le fichier Schematron est introuvable ou invalide.");
                }

                // Exécution du schematron sur la facture
                var schResult = schematron
                        .applySchematronValidationToSVRL(new StreamSource(new ByteArrayInputStream(xmlBytes)));

                if (schResult == null) {
                    resultats.add("❌ Erreur interne lors de l'application du Schematron.");
                    return resultats;
                }

                // Extraction des erreurs (SVRL Failed Assertions)
                var failedAsserts = SVRLHelper.getAllFailedAssertions(schResult);
                if (failedAsserts.isEmpty()) {
                    resultats.add("✅ Validation Schematron EN16931 réussie : Aucune erreur métier !");
                } else {
                    resultats
                            .add("❌ Échec Schematron : " + failedAsserts.size() + " règle(s) métier non respectée(s).");
                    // Dans une vraie app, on listerait chaque erreur ici, mais pour commencer c'est
                    // parfait.
                }
            }

        } catch (Exception e) {
            resultats.add("❌ Erreur pendant le traitement Schematron : " + e.getMessage());
        }

        return resultats;
    }
}
