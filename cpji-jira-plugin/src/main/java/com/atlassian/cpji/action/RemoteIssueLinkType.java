package com.atlassian.cpji.action;

/**
 * @since 2.1
 */
public enum RemoteIssueLinkType
{
    /**
     * Both directions
     */
    RECIPROCAL("cpji.remote.link.reciprocal"),
    /**
     * From copy to original issue
     */
    INCOMING("cpji.remote.link.incoming"),
    /**
     * From original to copy
     */
    OUTGOING("cpji.remote.link.outgoing"),
    NONE("cpji.remote.link.nolink");

    private final String i18nKey;

    RemoteIssueLinkType(String i18nKey)
    {
        this.i18nKey = i18nKey;
    }

    public String getI18nKey()
    {
        return i18nKey;
    }

    public boolean hasRemoteIssueLinkToLocal(){
        return equals(RECIPROCAL) || equals(INCOMING);
    }


    public boolean hasLocalIssueLinkToRemote(){
        return equals(RECIPROCAL) || equals(OUTGOING);
    }
}
