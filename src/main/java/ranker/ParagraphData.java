package ranker;

public class ParagraphData {
    public long location;
    public String paragraph;
    public String exactWord;
    public long hash;
    public int refCount = 1;

    @Override
    public int hashCode() {
        return (int)(hash >> 32);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        return this.hash == ((ParagraphData) o).hash;
    }
}
