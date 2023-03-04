package com.networknt.rule.soap.transformer;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.rule.RuleActionValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashMap;

public final class TransformChain {

    private static final Logger logger = LoggerFactory.getLogger(TransformChain.class);


    /* Head, Tail, and Final Transformers */
    Transformer h, t, f = null;

    private TransformChain() {
       // only create transform managers through static builder.
    }

    /**
     * Adds nodes to the doubly-linked list.
     *
     * @param tr - transformer to be added.
     */
    public void addTransformer(Transformer tr) {
        if (this.h == null) {
            this.h = this.t = tr;
            this.h.p = null;
        } else {
            this.t.n = tr;
            tr.p = this.t;
            this.t = tr;
        }
        this.t.n = null;
    }

    /**
     * Get the final result in ObjectNode form.
     *
     * @return - ObjectNode result after transformers are executed.
     */
    public JsonNode getResultObject() {
        if (this.f == null) {
            this.executeChain();
        }
        return this.f.finalizedObject;
    }

    public Transformer getExecutedChainTransformerResult() {
        return this.executeChain();
    }

    /**
     * Get the final result in String form.
     *
     * @return - String result after transformers are executed.
     */
    public String getResultString() {
        if (this.f == null) {
            this.executeChain();
        }
        return this.f.getAsString();
    }

    public Transformer executeChain() {
        return this.executeChain(this.h);
    }

    /**
     * Executes our transformer chain
     *
     * @return - returns the last Transformer in the chain in finished state.
     */
    private Transformer executeChain(Transformer start) {
        Transformer tr = start;
        while (true) {
            tr.init();
            tr.doTransform();

            if (logger.isDebugEnabled())
                logger.debug("start object: {}", tr.base);

            if (tr.n == null) {

                if (logger.isDebugEnabled())
                    logger.debug("final object: {}", tr.endTransitionState);

                return this.executeLastLink(tr);
            } else if (!tr.isFinalizedObjectRequired()) {
                tr.passToNextTransformer();
            } else {
                throw new RuntimeException("There are transformers remaining, but the final object has been finalized.");
            }

            if (logger.isDebugEnabled())
                logger.debug("end object: {}", tr.endTransitionState);

            tr = tr.n;
        }
    }

    private Transformer executeLastLink(Transformer t) {
        t.finalizeObject();
        this.f = t;
        return t;
    }

    public static TransformChain createNewChain(LinkedHashMap<String, Object> m, Collection<RuleActionValue> av) {
        var transformManager = new TransformChain();
        for (var a : av) {
            var tr = Transformer.getTransformerFromId(a, m);
            if (tr != null) {
                transformManager.addTransformer(tr);
            }
        }
        return transformManager;
    }

}
