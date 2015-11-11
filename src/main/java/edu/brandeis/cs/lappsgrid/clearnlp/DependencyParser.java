package edu.brandeis.cs.lappsgrid.clearnlp;

import edu.emory.clir.clearnlp.dependency.DEPNode;
import edu.emory.clir.clearnlp.dependency.DEPTree;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;

import java.util.ArrayList;
import java.util.List;

import static org.lappsgrid.discriminator.Discriminators.Uri;

@org.lappsgrid.annotations.ServiceMetadata(
        description = "ClearNLP Dependency Parser",
        requires_format = { "text", "lif" },
        produces_format = { "lif" },
        // TODO 151104 fix dependency structure to use alias
        produces = { "dependency", "http://vocab.lappsgrid.org/DependencyStructure", "token" }
)
public class DependencyParser extends AbstractClearNLPWebService {

    public DependencyParser() {
        super();
        // need POS as well for performance
        getComponent(POS);
        getComponent(DEP);
    }

    @Override
    public String execute(Container container) throws Exception {
        String text = container.getText();
        View view = container.newView();
        String serviceName = this.getClass().getName();
        view.addContains(Uri.DENDENCY_STRUCTURE,
                String.format("%s:%s", serviceName, getVersion()),
                "dependency-parser:clearnlp");
        view.addContains(Uri.DEPENDENCY ,
                String.format("%s:%s", serviceName, getVersion()),
                "dependency-parser:clearnlp");
        view.addContains(Uri.TOKEN ,
                String.format("%s:%s", serviceName, getVersion()),
                "tokenizer:clearnlp");
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
//                tok.addFeature(Uri.POS, token.getPOSTag());
//                tok.addFeature("word", token.getWordForm());

            }
            Annotation dep = view.newAnnotation(
                    makeID(DS_ID, sid), Uri.DENDENCY_STRUCTURE);
            List<String> dependencies = new ArrayList<>();
            for (DEPNode token : sent) {
                int did = token.getID();
                String depID = makeID(DEPENDENCY_ID, sid, did);
                dependencies.add(depID);

                Annotation dependency = view.newAnnotation(depID, Uri.DEPENDENCY);
                dependency.setLabel(token.getLabel());
                DEPNode head = token.getHead();
                dependency.addFeature("governor", makeID(TOKEN_ID, sid, head.getID()));
                dependency.addFeature("governor_word", head.getWordForm());
                dependency.addFeature("dependent", makeID(TOKEN_ID, sid, did));
                dependency.addFeature("dependent_word", token.getWordForm());

            }
            dep.getFeatures().put("dependencies", dependencies);
            sid++;
        }

        Data<Container> data = new Data<>(Uri.LIF, container);
        return Serializer.toJson(data);
    }

}
