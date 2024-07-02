package org.rapla.plugin.wwi2021;

import javax.ws.rs.FormParam;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.providers.multipart.PartType;
import java.io.InputStream;

public class ICSFileUploadForm {

    @FormParam("file")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private InputStream icsFile;

    public InputStream getIcsFile() {
        return icsFile;
    }

    public void setIcsFile(InputStream icsFile) {
        this.icsFile = icsFile;
    }
}
