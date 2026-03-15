package openclaw.memory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;

/**
 * Embedding value object representing a vector embedding.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record Embedding(
        float[] vector,
        String model,
        int dimension
) {

    /**
     * Create an embedding from a float array.
     *
     * @param vector the embedding vector
     * @param model the model used to generate the embedding
     */
    public Embedding {
        if (vector == null) {
            throw new IllegalArgumentException("Vector cannot be null");
        }
        dimension = vector.length;
    }

    /**
     * Create an embedding with default model name.
     *
     * @param vector the embedding vector
     */
    public Embedding(float[] vector) {
        this(vector, "unknown", vector.length);
    }

    /**
     * Get the vector as a float array.
     *
     * @return the vector
     */
    @Override
    public float[] vector() {
        return vector.clone();
    }

    /**
     * Convert the embedding to bytes.
     *
     * @return byte array representation
     */
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * 4);
        for (float v : vector) {
            buffer.putFloat(v);
        }
        return buffer.array();
    }

    /**
     * Create an embedding from bytes.
     *
     * @param bytes the byte array
     * @param model the model name
     * @return the embedding
     */
    public static Embedding fromBytes(byte[] bytes, String model) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        float[] vector = new float[bytes.length / 4];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = buffer.getFloat();
        }
        return new Embedding(vector, model, vector.length);
    }

    /**
     * Convert the embedding to a Base64 string.
     *
     * @return Base64 encoded string
     */
    public String toBase64() {
        return Base64.getEncoder().encodeToString(toBytes());
    }

    /**
     * Create an embedding from a Base64 string.
     *
     * @param base64 the Base64 string
     * @param model the model name
     * @return the embedding
     */
    public static Embedding fromBase64(String base64, String model) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        return fromBytes(bytes, model);
    }

    /**
     * Calculate cosine similarity with another embedding.
     *
     * @param other the other embedding
     * @return cosine similarity score (0 to 1)
     */
    public double cosineSimilarity(Embedding other) {
        if (this.vector.length != other.vector.length) {
            throw new IllegalArgumentException("Embeddings must have same dimension");
        }

        double dotProduct = 0;
        double normA = 0;
        double normB = 0;

        for (int i = 0; i < this.vector.length; i++) {
            dotProduct += this.vector[i] * other.vector[i];
            normA += this.vector[i] * this.vector[i];
            normB += other.vector[i] * other.vector[i];
        }

        if (normA == 0 || normB == 0) {
            return 0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Calculate Euclidean distance to another embedding.
     *
     * @param other the other embedding
     * @return Euclidean distance
     */
    public double euclideanDistance(Embedding other) {
        if (this.vector.length != other.vector.length) {
            throw new IllegalArgumentException("Embeddings must have same dimension");
        }

        double sum = 0;
        for (int i = 0; i < this.vector.length; i++) {
            double diff = this.vector[i] - other.vector[i];
            sum += diff * diff;
        }

        return Math.sqrt(sum);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Embedding embedding = (Embedding) o;
        return dimension == embedding.dimension &&
                Arrays.equals(vector, embedding.vector) &&
                model.equals(embedding.model);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(vector);
        result = 31 * result + model.hashCode();
        result = 31 * result + dimension;
        return result;
    }

    @Override
    public String toString() {
        return "Embedding{" +
                "dimension=" + dimension +
                ", model='" + model + '\'' +
                ", vector=[...]" +
                '}';
    }
}
