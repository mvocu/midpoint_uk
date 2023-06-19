import org.identityconnectors.framework.common.objects.ObjectClass

class BaseScript extends Script {

    public static final String PERSON_NAME = "Person"
    public static final ObjectClass PERSON = new ObjectClass(BaseScript.PERSON_NAME)

    @Override
    Object run() {
        return null
    }
}
