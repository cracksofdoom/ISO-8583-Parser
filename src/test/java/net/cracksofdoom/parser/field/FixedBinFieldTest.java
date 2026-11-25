/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.cracksofdoom.parser.field;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.xml.bind.JAXBException;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.cracksofdoom.parser.Strings;
import net.cracksofdoom.parser.field.Field;
import net.cracksofdoom.parser.field.FixedBinField;
import net.cracksofdoom.parser.field.IsoField;

/**
 *
 * @author bmg
 */
public class FixedBinFieldTest {

    @Test
    public void testEncode() {
        Logger logger = Logger.getLogger(FixedNumFieldTest.class);
        logger.info("Encoding FixedBinField ...");
        try {
            byte[] bytes;
            String lines;
            URL resource = FixedNumFieldTest.class.getResource("/iso_field.xml");
            Assertions.assertNotNull(resource);
            bytes = Files.readAllBytes(Paths.get(resource.toURI()));
            lines = new String(bytes, StandardCharsets.ISO_8859_1);
            IsoField isoField = Strings.unmarshal(lines, IsoField.class, "application/xml");
            isoField = isoField.getIsoFields().get(1);
            Field field = new FixedBinField(isoField);
            field.setValue("1000001000111000000000000000000000000000000000000000000000000000");
            String encodedValue = field.encode();
            logger.infof("Encoded value => %s", encodedValue);
            field.setValue("");
            field.decode(encodedValue);
            Assertions.assertEquals("1000001000111000000000000000000000000000000000000000000000000000",
                    field.getValue());
        } catch (JAXBException | IOException | RuntimeException | URISyntaxException ex) {
            logger.error(ex);
            Assertions.fail();
        }

    }

    @Test
    public void testDecode() {
        Logger logger = Logger.getLogger(FixedNumFieldTest.class);
        logger.info("Decoding FixedBinField ...");
        try {
            byte[] bytes;
            String lines;
            URL resource = FixedNumFieldTest.class.getResource("/iso_field.xml");
            Assertions.assertNotNull(resource);
            bytes = Files.readAllBytes(Paths.get(resource.toURI()));
            lines = new String(bytes, StandardCharsets.ISO_8859_1);
            IsoField isoField = Strings.unmarshal(lines, IsoField.class, "application/xml");
            isoField = isoField.getIsoFields().get(1);
            URL resource1 = FixedNumFieldTest.class.getResource("/echo.log");
            Assertions.assertNotNull(resource1);
            bytes = Files.readAllBytes(Paths.get(resource1.toURI()));
            lines = new String(bytes, StandardCharsets.ISO_8859_1);
            Field field = new FixedBinField(isoField);
            field.decode(lines.substring(4));
            logger.infof("Value => %s", field.getValue());
            Assertions.assertEquals("1000001000111000000000000000000000000000000000000000000000000000",
                    field.getValue());
        } catch (JAXBException | IOException | RuntimeException | URISyntaxException ex) {
            logger.error(ex);
            Assertions.fail();
        }

    }
}
