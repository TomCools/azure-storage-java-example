package be.tomcools.azurestorage;

import org.testcontainers.containers.GenericContainer;

/**
 * Testcontainer GenericContainer class which will be responsible for starting and stopping the Azurite docker container.
 * It by default enables Blob, Queue and Table storage systems and exposes them on their default ports.
 */
public class AzureStorageContainer extends GenericContainer<AzureStorageContainer> {

    // Port defaults here are documented on Github: https://github.com/Azure/Azurite?tab=readme-ov-file#run-azurite-v3-docker-image
    public static final int DEFAULT_BLOB_PORT = 10000;
    public static final int DEFAULT_QUEUE_PORT = 10001;
    public static final int DEFAULT_TABLE_PORT = 10002;

    // Credential defaults here are documented on Github: https://github.com/Azure/Azurite?tab=readme-ov-file#default-storage-account
    private final static String DEFAULT_ACCOUNT_NAME = "devstoreaccount1";
    private final static String DEFAULT_ACCOUNT_KEY = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";

    private String accountName;
    private String accountKey;

    public AzureStorageContainer() {
        super("mcr.microsoft.com/azure-storage/azurite");

        this.accountName = DEFAULT_ACCOUNT_NAME;
        this.accountKey = DEFAULT_ACCOUNT_KEY;
    }

    @Override
    public void start() {
        withEnv("AZURITE_ACCOUNTS", "%s:%s".formatted(accountName, accountKey));

        withExposedPorts(DEFAULT_BLOB_PORT, DEFAULT_QUEUE_PORT, DEFAULT_TABLE_PORT);
        super.start();
    }

    public String getConnectionString() {
        return "DefaultEndpointsProtocol=http;AccountName=" +
                accountName +
                ";AccountKey=" +
                accountKey +
                ";BlobEndpoint=http://127.0.0.1:" +
                getMappedPort(DEFAULT_BLOB_PORT) +
                "/" +
                accountName +
                ";QueueEndpoint=http://127.0.0.1:" +
                getMappedPort(DEFAULT_QUEUE_PORT) +
                "/" +
                accountName +
                ";TableEndpoint=http://127.0.0.1:" +
                getMappedPort(DEFAULT_TABLE_PORT) +
                "/" +
                accountName +
                ";";
    }

    public AzureStorageContainer withAccountName(String accountName) {
        this.accountName = accountName;
        return self();
    }

    public AzureStorageContainer withAccountKey(String accountKey) {
        this.accountKey = accountKey;
        return self();
    }
}

