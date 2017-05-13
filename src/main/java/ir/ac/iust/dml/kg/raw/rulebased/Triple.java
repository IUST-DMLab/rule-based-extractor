package ir.ac.iust.dml.kg.raw.rulebased;

/**
 * @author Mohammad Abdous md.abdous@gmail.com
 * @version 1.1.0
 * @since 5/4/17 11:55 AM
 */
public class Triple {

    String subject;
    String object;
    String predicate;
    String sentence;
    String objectUri;
    String subjectUri;

    public String getObjectUri() {
        return objectUri;
    }

    public void setObjectUri(String objectUri) {
        this.objectUri = objectUri;
    }

    public String getSubjectUri() {
        return subjectUri;
    }

    public void setSubjectUri(String subjectUri) {
        this.subjectUri = subjectUri;
    }


    public String getSentence() {
        return sentence;
    }

    public void setSentence(String sentence) {
        this.sentence = sentence;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getPredicate() {
        return predicate;
    }

    public void setPredicate(String predicate) {
        this.predicate = predicate;
    }

    public String toString() {
        return "("+ subject+","+predicate+","+object+")";
    }
}
