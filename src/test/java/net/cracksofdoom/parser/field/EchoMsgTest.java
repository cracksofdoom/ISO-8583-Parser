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

/**
 *
 * @author bmg
 */
public class EchoMsgTest {

    @Test
    public void testEncode() {
        Logger logger = Logger.getLogger(FixedNumFieldTest.class);
        logger.info("Encoding Network Message ...");
        try {
            byte[] bytes;
            String lines;
            URL resource = FixedNumFieldTest.class.getResource("/iso_field.xml");
            Assertions.assertNotNull(resource);
            bytes = Files.readAllBytes(Paths.get(resource.toURI()));
            lines = new String(bytes, StandardCharsets.ISO_8859_1);
            IsoField isoField = Strings.unmarshal(lines, IsoField.class, "application/xml");
            CompoundField field = new FixedCompoundField(isoField);
            field.setValue(0, "0800");// MTI
            field.setValue(7, "1124210025"); // Date and Time
            field.setValue(11, "254513"); // STAN
            field.setValue(70, "301"); // Management Information Code
            String encodedValue = field.encode();
            logger.infof("Encoded value => %s", encodedValue);
            field.setValue("");
            field.decode(encodedValue);
            Assertions.assertEquals("0800", field.getValue(0));
            Assertions.assertEquals("1124210025", field.getValue(7));
            Assertions.assertEquals("254513", field.getValue(11));
            Assertions.assertEquals("301", field.getValue(70));
        } catch (JAXBException | IOException | RuntimeException | URISyntaxException ex) {
            logger.error(ex);
            Assertions.fail();
        }

    }

}
