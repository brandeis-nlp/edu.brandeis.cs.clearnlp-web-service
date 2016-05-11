package edu.brandeis.cs.lappsgrid.clearnlp;

import edu.brandeis.cs.lappsgrid.clearnlp.impl.Tokenizer;
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
public class TestTokenizer extends TestService {

    String testSent = "Hello World.";

    public TestTokenizer() throws Exception {
        service = new Tokenizer();
    }

    @Test
    public void testMetadata(){
        ServiceMetadata metadata = super.testCommonMetadata();
        IOSpecification produces = metadata.getProduces();
        IOSpecification requires = metadata.getRequires();
        assertEquals("Expected 1 annotation, found: " + produces.getAnnotations().size(),
                1, produces.getAnnotations().size());
        assertTrue("Tokens not produced", produces.getAnnotations().contains(Uri.TOKEN));
    }

    @Test
    public void testExecute(){

        String result0 = service.execute(testSent);
        String input = new Data<>(Uri.LIF, wrapContainer(testSent)).asJson();
        String result = service.execute(input);
        Assert.assertEquals(result0, result);

        System.out.println("<------------------------------------------------------------------------------");
        System.out.println(result);
        System.out.println("------------------------------------------------------------------------------>");

        Container resultContainer = reconstructPayload(result);
        assertEquals("Text is corrupted.", resultContainer.getText(), testSent);
        List<View> views = resultContainer.getViews();
        if (views.size() != 1) {
            fail(String.format("Expected 1 view. Found: %d", views.size()));
        }
        View view = resultContainer.getView(0);
        assertTrue("Not containing tokens", view.contains(Uri.TOKEN));
        List<Annotation> annotations = view.getAnnotations();
        if (annotations.size() != 3) {
            fail(String.format("Expected 3 token. Found: %d", annotations.size()));
        }
        System.out.println(Serializer.toPrettyJson(resultContainer));
    }

    @Test
    public void canTokenizePluralSentences() {
        String testSents = "The attack occurred just before 8 a.m. outside a building that houses humanities and natural sciences classrooms, among others, said school spokeswoman Lorena Anderson. She could not say if the attacker or the victims were students. “I can tell you that we’re really shocked and saddened by this,” Anderson said. “We’re doing everything we can to contact family and parents to make sure everyone here is safe and secure.”";
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
