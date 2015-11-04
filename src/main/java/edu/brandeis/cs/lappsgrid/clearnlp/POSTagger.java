package edu.brandeis.cs.lappsgrid.clearnlp;

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
        description = "ClearNLP Part-of-speech Tagger",
        requires_format = { "text", "lif" },
        produces_format = { "lif" },
        produces = { "pos" }
)
public class POSTagger extends AbstractClearNLPWebService {

    public POSTagger() {
        super();
        getComponent(POS);
    }

    @Override
    public String execute(Container container) throws Exception {
        String text = container.getText();
        View view = container.newView();
        String serviceName = this.getClass().getName();
        view.addContains(Uri.POS,
                String.format("%s:%s", serviceName, getVersion()),
                "pos-tagger:clearnlp");
        List<DEPTree> sents = process(text);
        List<int[]> spans = getSpans(text);
        int tokenSoFar = 0;
        int sid = 1;
        for (DEPTree sent : sents) {
            for (DEPNode token : sent) {
                int tid = token.getID();
                if (tid == 0) { continue; } // root node
                int[] span = spans.get(tokenSoFar++);
                int start = span[0]; int end = span[1];
                Annotation tok = view.newAnnotation(
                        makeID(TOKEN_ID, sid, tid), Uri.TOKEN, start, end);
                tok.addFeature("pos", token.getPOSTag());
                tok.addFeature("word", token.getWordForm());

            }
            sid++;
        }

        Data<Container> data = new Data<>(Uri.LIF, container);
        return Serializer.toJson(data);
    }

}
