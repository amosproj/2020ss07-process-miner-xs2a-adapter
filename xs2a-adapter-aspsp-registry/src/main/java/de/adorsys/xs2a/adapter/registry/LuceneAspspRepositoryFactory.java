package de.adorsys.xs2a.adapter.registry;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import de.adorsys.xs2a.adapter.registry.exception.RegistryIOException;
import de.adorsys.xs2a.adapter.registry.mapper.AspspMapper;
import de.adorsys.xs2a.adapter.service.model.Aspsp;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.mapstruct.factory.Mappers;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class LuceneAspspRepositoryFactory {
    private static final String CSV_ASPSP_ADAPTER_CONFIG_FILE_PATH_PROPERTY = "csv.aspsp.adapter.config.file.path";
    private static final String DEFAULT_CSV_ASPSP_ADAPTER_CONFIG_FILE = "aspsp-adapter-config.csv";
    private static final String DEFAULT_LUCENE_DIR_PATH = "lucene";

    private final AspspMapper aspspMapper = Mappers.getMapper(AspspMapper.class);

    public LuceneAspspRepository newLuceneAspspRepository() {
        try {
            return newLuceneAspspRepositoryInternal();
        } catch (IOException e) {
            throw new RegistryIOException(e);
        }
    }

    private LuceneAspspRepository newLuceneAspspRepositoryInternal() throws IOException {
        byte[] csv = getCsvFileAsByteArray();

        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        String computedDigest = new BigInteger(1, messageDigest.digest(csv)).toString(16);

        Path digestPath = Paths.get(DEFAULT_LUCENE_DIR_PATH, "digest.md5");
        boolean changed;
        if (Files.exists(digestPath)) {
            String storedDigest = new String(Files.readAllBytes(digestPath));
            changed = !computedDigest.equals(storedDigest);
        } else {
            changed = true;
        }

        if (changed) {
            Files.write(digestPath, computedDigest.getBytes());
        }

        Directory directory = FSDirectory.open(Paths.get(DEFAULT_LUCENE_DIR_PATH, "index"));
        LuceneAspspRepository luceneAspspRepository = new LuceneAspspRepository(directory);
        if (changed) {
            for (String f : directory.listAll()) {
                directory.deleteFile(f);
            }
            List<Aspsp> aspsps = aspspMapper.toAspsps(readAllRecords(csv));
            luceneAspspRepository.saveAll(aspsps);
        }
        return luceneAspspRepository;
    }

    private byte[] getCsvFileAsByteArray() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[8192];
        try (InputStream is = getCsvFileAsStream()) {
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        }
    }

    private InputStream getCsvFileAsStream() throws FileNotFoundException {
        String filePath = System.getenv(CSV_ASPSP_ADAPTER_CONFIG_FILE_PATH_PROPERTY);

        InputStream inputStream;

        if (filePath == null || filePath.isEmpty()) {
            inputStream = getResourceAsStream(DEFAULT_CSV_ASPSP_ADAPTER_CONFIG_FILE);
        } else {
            inputStream = getFileAsStream(filePath);
        }
        return inputStream;
    }

    private InputStream getResourceAsStream(String fileName) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
    }

    private InputStream getFileAsStream(String filePath) throws FileNotFoundException {
        return new FileInputStream(filePath);
    }

    private List<AspspCsvRecord> readAllRecords(byte[] csv) throws IOException {

        List<AspspCsvRecord> records = new CsvMapper().readerWithTypedSchemaFor(AspspCsvRecord.class)
                .<AspspCsvRecord>readValues(csv)
                .readAll();
        return records;
    }
}
