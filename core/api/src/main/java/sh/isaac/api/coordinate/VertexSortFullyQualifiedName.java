package sh.isaac.api.coordinate;

import sh.isaac.api.externalizable.ByteArrayDataBuffer;
import sh.isaac.api.marshal.Marshalable;
import sh.isaac.api.marshal.Marshaler;
import sh.isaac.api.marshal.Unmarshaler;

import java.util.Optional;
import java.util.UUID;

public class VertexSortFullyQualifiedName extends VertexSortAbstract implements Marshalable {

    private static final int marshalVersion = 1;

    private static final UUID VERTEX_SORT_UUID = UUID.fromString("998489ec-6eca-11ea-bc55-0242ac130003");

    public static final VertexSortFullyQualifiedName SINGLETON = new VertexSortFullyQualifiedName();

    private VertexSortFullyQualifiedName() {
    }

    @Override
    public UUID getVertexSortUUID() {
        return VERTEX_SORT_UUID;
    }

    @Override
    public String getVertexSortName() {
        return "Sort by fully qualified name";
    }

    public String getVertexLabel(int vertexConceptNid, LanguageCoordinate languageCoordinate, StampFilter stampFilter) {
        return getFullyQualifiedName(vertexConceptNid, languageCoordinate, stampFilter);
    }

    protected static String getFullyQualifiedName(int vertexConceptNid, LanguageCoordinate languageCoordinate, StampFilter stampFilter) {
        Optional<String> optionalName = languageCoordinate.getFullyQualifiedNameText(vertexConceptNid, stampFilter);
        if (optionalName.isPresent()) {
            return optionalName.get();
        }
        optionalName = languageCoordinate.getPreferredDescriptionText(vertexConceptNid, stampFilter);
        if (optionalName.isPresent()) {
            return optionalName.get();
        }
        return languageCoordinate.getAnyName(vertexConceptNid, stampFilter);
    }

    @Unmarshaler
    public static Object make(ByteArrayDataBuffer in) {
        int objectMarshalVersion = in.getInt();
        switch (objectMarshalVersion) {
            case marshalVersion:
                // Using a static method rather than a constructor eliminates the need for
                // a readResolve method, but allows the implementation to decide how
                // to handle special cases. This is the equivalent of readresolve, since it
                // returns an existing object always.
                return SINGLETON;
            default:
                throw new UnsupportedOperationException("Unsupported version: " + objectMarshalVersion);
        }
    }

    @Override
    @Marshaler
    public void marshal(ByteArrayDataBuffer out) {
        out.putInt(marshalVersion);
        // No fields...
    }
}
