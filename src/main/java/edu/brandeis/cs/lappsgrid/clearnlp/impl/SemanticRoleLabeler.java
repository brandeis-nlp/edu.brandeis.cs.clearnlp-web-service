package edu.brandeis.cs.lappsgrid.clearnlp.impl;

import edu.brandeis.cs.lappsgrid.clearnlp.AbstractClearNLPWebService;
import edu.emory.clir.clearnlp.dependency.DEPNode;
import edu.emory.clir.clearnlp.dependency.DEPTree;
import edu.emory.clir.clearnlp.util.arc.SRLArc;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;

import java.util.List;

import static org.lappsgrid.discriminator.Discriminators.Uri;

@org.lappsgrid.annotations.ServiceMetadata(
        description = "ClearNLP Semantic Role Labeler",
        requires_format = { "text", "lif" },
        produces_format = { "lif" },
        produces = { "semantic-role", "token" }
)
public class SemanticRoleLabeler extends AbstractClearNLPWebService {

    public SemanticRoleLabeler() {
        super();
        // need POS as well for performance
        getComponent(POS);
        getComponent(MORPH);
        getComponent(DEP);
        getComponent(SRL);
    }

    @Override
    public String execute(Container container) throws Exception {
        String text = container.getText();
        View view = container.newView();
        String serviceName = this.getClass().getName();
        view.addContains(Uri.SEMANTIC_ROLE,
                String.format("%s:%s", serviceName, getVersion()),
                "semantic-role-labeler:clearnlp");
        view.addContains(Uri.TOKEN ,
                String.format("%s:%s", serviceName, getVersion()),
                "tokenizer:clearnlp");
        List<DEPTree> sents = process(text);
        List<int[]> spans = getSpans(text);
        int tokenSoFar = 0;
        int sid = 1;
        for (DEPTree sent : sents) {
            System.out.println(sent);
            for (DEPNode token : sent) {
                int tid = token.getID();

                if (tid == 0) { continue; } // root node

                int[] span = spans.get(tokenSoFar++);
                int start = span[0]; int end = span[1];
                view.newAnnotation(makeID(TOKEN_ID_PREFIX, sid, tid), Uri.TOKEN, start, end);

            }
            int srlId = 0;
            for (DEPNode token : sent) {
                List<SRLArc> semRoles = token.getSemanticHeadArcList();
                if (semRoles.size() == 0) { continue; }
                int argTokenIdx = token.getID();
                String argTokenId = makeID(TOKEN_ID_PREFIX, sid, argTokenIdx);
                for (SRLArc role : semRoles) {
                    int headTokenIdx = role.getNode().getID();
                    String headTokenId = makeID(TOKEN_ID_PREFIX, sid, headTokenIdx);

                    Annotation semRole = view.newAnnotation(
                            makeID(SEMROLE_ID_PREFIX, sid, srlId++),
                            Uri.SEMANTIC_ROLE);
                    semRole.addFeature("head", headTokenId);
                    semRole.addFeature("argument", argTokenId);
                    semRole.addFeature("label", role.getLabel());
                }
            }
            sid++;
        }

        Data<Container> data = new Data<>(Uri.LIF, container);
        return Serializer.toJson(data);
    }

}
