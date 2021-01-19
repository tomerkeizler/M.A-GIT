package xmlHandlers;

import Generated.MagitRepository;
import org.apache.commons.io.FileUtils;
import javax.xml.bind.*;
import java.io.IOException;
import java.io.InputStream;


public class JAXB {

    private static MagitRepository deserializeRepositoryFromXML(InputStream in) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(MagitRepository.class);
        Unmarshaller u = jc.createUnmarshaller();
        return (MagitRepository) u.unmarshal(in);
    }

    public static MagitRepository loadXML(String xmlFilePath) throws IOException {
        InputStream inputStream = FileUtils.openInputStream(FileUtils.getFile(xmlFilePath));
        try {
            return deserializeRepositoryFromXML(inputStream);
        } catch (JAXBException e) {
            e.printStackTrace();
            return null;
        }
    }





}
