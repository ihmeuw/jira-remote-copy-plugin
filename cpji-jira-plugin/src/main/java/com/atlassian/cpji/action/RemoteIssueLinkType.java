package com.atlassian.cpji.action;

/**
 * @since 2.1
 */
public enum RemoteIssueLinkType
{
    RECIPROCAL("cpji.remote.link.reciprocal"),
    INCOMING("cpji.remote.link.incoming"),
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
        return equals(RECIPROCAL) || equals(OUTGOING);
    }


    public boolean hasLocalIssueLinkToRemote(){
        return equals(RECIPROCAL) || equals(INCOMING);
    }
}
