package com.saucer.sast;

import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.apache.commons.text.lookup.StringLookupFactory;

import java.util.HashMap;
import java.util.Map;

public class interpolator_commons_text {

    public void commonstext0() {
        String payload = "${script:javascript:java.lang.Runtime.getRuntime().exec(\"open -a Calculator.app\")}";
        final StringSubstitutor interpolator = StringSubstitutor.createInterpolator();
        final String text = interpolator.replace(payload);
    }

    public void commonstext1(String payload) {
        final StringSubstitutor interpolator = StringSubstitutor.createInterpolator();
        final String text = interpolator.replace(payload);
    }

    public void commonstext2(String payload) {
        StringSubstitutor.createInterpolator().replace(payload);
    }

    public void commonstext3(String payload) {
        final StringSubstitutor interpolator = new StringSubstitutor(StringLookupFactory.INSTANCE.interpolatorStringLookup());
        final String text = interpolator.replace(payload);
    }

    public void commonstext4(String payload) {
        new StringSubstitutor(StringLookupFactory.INSTANCE.interpolatorStringLookup()).replace(payload);
    }

    public void commonstext5(String payload) {
        StringLookupFactory.INSTANCE.scriptStringLookup().lookup(payload);
    }

    public void commonstextWithPatch(String payload) {
        Map<String, StringLookup> lookupMap = new HashMap<>();
        lookupMap.put("script", StringLookupFactory.INSTANCE.scriptStringLookup());
        StringLookup variableResolver = StringLookupFactory.INSTANCE.interpolatorStringLookup(lookupMap, null, false);
        String value = new StringSubstitutor(variableResolver).replace(payload);
    }

}
