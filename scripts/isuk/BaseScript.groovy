import org.identityconnectors.framework.common.objects.ObjectClass

class BaseScript extends Script {

    public static final String PERSON_NAME = "Person"
    public static final ObjectClass PERSON = new ObjectClass(BaseScript.PERSON_NAME)

    public static final String GROUP_NAME = "Group"
    public static final ObjectClass GROUP = new ObjectClass(BaseScript.GROUP_NAME)

    public static final String ORGANIZATION_NAME = "Organization"
    public static final ObjectClass ORGANIZATION = new ObjectClass(BaseScript.ORGANIZATION_NAME)

    public static final String RELATION_NAME = "Relation"
    public static final ObjectClass RELATION = new ObjectClass(BaseScript.RELATION_NAME)

    @Override
    Object run() {
        return null
    }
}
