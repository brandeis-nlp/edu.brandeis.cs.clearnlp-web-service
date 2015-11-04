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
        description = "ClearNLP Named Entity Recognizer",
        requires_format = { "text", "lif" },
        produces_format = { "lif" },
        produces = { "person", "location", "date", "organization" }
)
public class NamedEntityRecognizer extends AbstractClearNLPWebService {

    public NamedEntityRecognizer() {
        super();
        getComponent(NER);
    }

    @Override
    public String execute(Container container) throws Exception {
        String text = container.getText();
        View view = container.newView();
        String serviceName = this.getClass().getName();
        view.addContains(Uri.NE,
                String.format("%s:%s", serviceName, getVersion()),
                "ner:clearnlp");
        List<DEPTree> sents = process(text);
        List<int[]> spans = getSpans(text);
        int id = 1;
        int tokenSoFar = 0;
        int start = -1;
        int end;
        for (DEPTree sent : sents) {
            for (DEPNode token : sent) {

                if (token.getID() == 0) { continue; }

                String neTag = token.getNamedEntityTag();
                if (neTag.startsWith("O")) { continue; }

                if (start == -1) {
                    start = spans.get(tokenSoFar)[0];
                }

                // TODO 151104 need to collapse cNLP ne tagset
                String neType = neTag.substring(2).toLowerCase();
                if (neTag.startsWith("L") || neTag.startsWith("U")) {
                    end = spans.get(tokenSoFar)[1];
                    Annotation annotation = view.newAnnotation(
                            makeID(NE_ID, id++), neType, start, end);
                    annotation.addFeature("word", token.getWordForm());

                    start = -1;
                }
                tokenSoFar++;
            }
        }

        Data<Container> data = new Data<>(Uri.LIF, container);
        return Serializer.toJson(data);
    }

}
