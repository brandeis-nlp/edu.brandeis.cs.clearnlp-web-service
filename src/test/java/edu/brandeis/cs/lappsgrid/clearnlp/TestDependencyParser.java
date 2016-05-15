package edu.brandeis.cs.lappsgrid.clearnlp;

import edu.brandeis.cs.lappsgrid.clearnlp.impl.DependencyParser;
import junit.framework.Assert;
import org.junit.Test;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;

import java.util.List;

import static org.junit.Assert.*;
import static org.lappsgrid.discriminator.Discriminators.Uri;

/**
 * Copied from stanford-web-service
 * @author krim@brandeis.edu
 */
public class TestDependencyParser extends TestService {

    String testSent = "Hi, Programcreek is a very huge and useful website.";

    public TestDependencyParser() throws Exception {
        service = new DependencyParser();
    }


    @Test
    public void testMetadata() {
        ServiceMetadata metadata = super.testCommonMetadata();
        IOSpecification requires = metadata.getRequires();
        IOSpecification produces = metadata.getProduces();
        assertEquals("Expected 3 annotations, found: " + produces.getAnnotations().size(),
                3, produces.getAnnotations().size());
        assertTrue("Dependencies not produced",
                produces.getAnnotations().contains(Uri.DEPENDENCY));
        assertTrue("Dependency Structures not produced",
                produces.getAnnotations().contains(Uri.DEPENDENCY_STRUCTURE));
        assertTrue("Tokens not produced",
                produces.getAnnotations().contains(Uri.TOKEN));
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


        input = "Hi, Programcreek is a very huge and useful website.";
        result = service.execute(input);
        Container resultContainer = reconstructPayload(result);
        assertEquals("Text is corrupted.", resultContainer.getText(), testSent);
        List<View> views = resultContainer.getViews();
        if (views.size() != 1) {
            fail(String.format("Expected 1 view. Found: %d", views.size()));
        }
        View view = resultContainer.getView(0);
        assertTrue("Not containing dependency", view.contains(Uri.DEPENDENCY));
        assertTrue("Not containing dependency structure", view.contains(Uri.DEPENDENCY_STRUCTURE));
        assertTrue("Not containing tokens", view.contains(Uri.TOKEN));
        System.out.println(Serializer.toPrettyJson(resultContainer));
    }
}
