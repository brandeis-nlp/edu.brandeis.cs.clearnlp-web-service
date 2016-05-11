package edu.brandeis.cs.lappsgrid.clearnlp.impl;

import edu.brandeis.cs.lappsgrid.clearnlp.AbstractClearNLPWebService;
import edu.emory.clir.clearnlp.dependency.DEPNode;
import edu.emory.clir.clearnlp.dependency.DEPTree;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;

import java.util.List;

import static org.lappsgrid.discriminator.Discriminators.Uri;

@org.lappsgrid.annotations.ServiceMetadata(
        description = "ClearNLP Tokenizer",
        requires_format = { "text", "lif" },
        produces_format = { "lif" },
        produces = { "token" }
)
public class Tokenizer extends AbstractClearNLPWebService {

    public Tokenizer() {
        super();
    }

    @Override
    public String execute(Container container) throws Exception {
        String text = container.getText();
        View view = container.newView();
        String serviceName = this.getClass().getName();
        view.addContains(Uri.TOKEN,
                String.format("%s:%s", serviceName, getVersion()),
                "tokenizer:clearnlp");
        List<List<String>> sents = split(text);
        List<int[]> spans = getSpans(text);
        int tokenSoFar = 0;
        int sid = 1;
        for (List<String> sent : sents) {
            int tid = 1;
            for (String token : sent) {
                int[] span = spans.get(tokenSoFar++);
                int start = span[0]; int end = span[1];
                Annotation tok = view.newAnnotation(
                        makeID(TOKEN_ID, sid, tid++), Uri.TOKEN, start, end);
                tok.addFeature("word", token);
            }
            sid++;
        }

        Data<Container> data = new Data<>(Uri.LIF, container);
        return Serializer.toJson(data);
    }

}
