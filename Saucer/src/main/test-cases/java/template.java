import org.apache.commons.text.StringSubstitutor;

public class template {
    void commonsText(String payload) {
        StringSubstitutor stringSubstitutor = StringSubstitutor.createInterpolator();
        stringSubstitutor.replace(payload);
    }
}
