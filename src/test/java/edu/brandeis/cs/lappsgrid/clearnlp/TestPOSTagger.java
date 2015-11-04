package edu.brandeis.cs.lappsgrid.clearnlp;

import junit.framework.Assert;
import org.junit.Test;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;

import java.util.List;

import static org.junit.Assert.*;
import static org.lappsgrid.discriminator.Discriminators.Uri;

/**
 * Copied from stanford-web-service
 * @author krim@brandeis.edu
 */
public class TestPOSTagger extends TestService {

    String simpleTestSent = "Good morning.";
    // 72 tokens
    String testSents = "The attack occurred just before 8 a.m. outside a building that houses humanities and natural sciences classrooms, among others, said school spokeswoman Lorena Anderson. She could not say if the attacker or the victims were students. “I can tell you that we’re really shocked and saddened by this,” Anderson said. “We’re doing everything we can to contact family and parents to make sure everyone here is safe and secure.”";

    public TestPOSTagger() throws Exception {
        service = new POSTagger();
    }

    @Test
    public void testMetadata(){
        ServiceMetadata metadata = super.testCommonMetadata();
        IOSpecification produces = metadata.getProduces();
        IOSpecification requires = metadata.getRequires();
        assertEquals("Expected 1 annotation, found: " + produces.getAnnotations().size(),
                1, produces.getAnnotations().size());
        assertTrue("POS tags not produced", produces.getAnnotations().contains(Uri.POS));
    }

    @Test
    public void canTagPureText() {
        String resultFromPure = service.execute(testSents);
        String leds = new Data<>(Uri.LIF, wrapContainer(testSents)).asJson();
        String resultFromLEDS = service.execute(leds);
        assertEquals("Results from pure text and LEDS(TEXT) are different.",
                resultFromPure, resultFromLEDS);

    }
    @Test
    public void canTagSimpleSent(){
        String result = service.execute(simpleTestSent);
        Container resultCont = reconstructPayload(result);
        System.out.println("<------------------------------------------------------------------------------");
        System.out.println(result);
        System.out.println("------------------------------------------------------------------------------>");
        assertEquals("Text is corrupted.", simpleTestSent, resultCont.getText());
        List<View> views = resultCont.getViews();
        if (views.size() != 1) {
            fail(String.format("Expected 1 view. Found: %d", views.size()));
        }
        View view = resultCont.getView(0);
        assertTrue("View not containing POS tags", view.contains(Uri.POS));
        List<Annotation> annotations = view.getAnnotations();
        if (annotations.size() != 3) {
            fail(String.format("Expected 3 tokens. Found: %d", views.size()));
        }
        Annotation annotation = annotations.get(0);
        assertEquals("@type is should be TOKEN: " + annotation.getAtType(),
                Uri.TOKEN, annotation.getAtType());
        String goodPos = annotation.getFeature("pos");
        assertEquals("'Good' is a JJ. Found: " + goodPos, "JJ", goodPos);
        System.out.println(Serializer.toPrettyJson(resultCont));
    }
    @Test
    public void canTagPluralSentences() {
        String input = new Data<>(Uri.LIF, wrapContainer(testSents)).asJson();
        String result = service.execute(input);
        System.out.println("<------------------------------------------------------------------------------");
        System.out.println(result);
        System.out.println("------------------------------------------------------------------------------>");
        Container resultCont = reconstructPayload(result);
        List<Annotation> annotations = resultCont.getView(0).getAnnotations();
        assertEquals("Expected 82 tokens, found: " + annotations.size(), 82, annotations.size());
        System.out.println(Serializer.toPrettyJson(resultCont));

    }
}