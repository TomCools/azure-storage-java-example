package be.tomcools.azurestorage;

import be.tomcools.azurestorage.dto.ExampleDto;
import com.azure.core.util.BinaryData;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableServiceClient;
import com.azure.data.tables.TableServiceClientBuilder;
import com.azure.data.tables.models.TableEntity;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.azure.storage.queue.models.QueueMessageItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@Testcontainers
class AzureStorageTest {

    @Container // static field so the container is reused within the test set.
    private static final AzureStorageContainer AZURE_STORAGE_CONTAINER = new AzureStorageContainer();

    @Nested
    class BlobStorage {
        private final String TEST_BLOB_CONTAINER = "testblob";

        BlobServiceClient sut;

        @BeforeEach
        public void init() {
            sut = new BlobServiceClientBuilder()
                    .connectionString(AZURE_STORAGE_CONTAINER.getConnectionString())
                    .buildClient();

            sut.createBlobContainer(TEST_BLOB_CONTAINER);
        }

        @AfterEach
        public void cleanup() {
            sut.deleteBlobContainer(TEST_BLOB_CONTAINER);
        }

        @Test
        public void blobContainerCanNotBeInUpperCase() {
            assertThatThrownBy(() -> sut.createBlobContainer("UPPERCASE_NOT_SUPPORTED"))
                    .isOfAnyClassIn(BlobStorageException.class);
        }

        @Test
        public void givenSomeTestData_whenSavingToBlobAndRetrievingString_objectsShouldBeEqual() {
            BlobContainerClient client = sut.getBlobContainerClient(TEST_BLOB_CONTAINER);
            BlobClient blob = client.getBlobClient("blob");

            String testString = "TEST_DATA";
            blob.upload(BinaryData.fromString(testString));
            BinaryData downloadContent = blob.downloadContent();

            assertThat(testString).isEqualTo(downloadContent.toString());
        }
    }

    @Nested
    class TableStorage {
        private final String TEST_TABLE = "TESTTABLE";
        private final String TEST_PARTITION = "TEST_PARTITION";
        private final String TEST_ROW = "TEST_ROW";

        TableServiceClient sut;

        @BeforeEach
        public void init() {
            this.sut = new TableServiceClientBuilder()
                    .connectionString(AZURE_STORAGE_CONTAINER.getConnectionString())
                    .buildClient();
            sut.createTable(TEST_TABLE);
        }

        @AfterEach
        public void cleanup() {
            sut.deleteTable(TEST_TABLE);
        }

        @Test
        public void givenSomeTestData_whenSavingToTableAndRetrievingString_objectsShouldBeEqual() {
            final TableClient test = sut.getTableClient(TEST_TABLE);

            TableEntity entity = new TableEntity(TEST_PARTITION, TEST_ROW)
                    .addProperty("string", "TEST_STRING");


            test.createEntity(entity);
            final TableEntity result = test.getEntity(TEST_PARTITION, TEST_ROW);

            assertThat(result.getProperty("string")).isEqualTo("TEST_STRING");
        }

        @Test
        public void givenSomeTestData_whenSavingToTableAndRetrievingDate_objectsShouldBeEqual() {
            final TableClient test = sut.getTableClient(TEST_TABLE);

            LocalDateTime TEST_TIME = LocalDateTime.now();

            TableEntity entity = new TableEntity(TEST_PARTITION, TEST_ROW)
                    .addProperty("datetime", TEST_TIME);

            test.createEntity(entity);
            final TableEntity result = test.getEntity(TEST_PARTITION, TEST_ROW);

            // So, the library returns an OffsetDateTime, unexpected, but we have a test to verify that behaviour now! :-)
            assertThat(((OffsetDateTime) result.getProperty("datetime")).toLocalDateTime()).isEqualTo(TEST_TIME);
        }
    }

    @Nested
    class QueueStorage {
        private final String TEST_QUEUE_NAME = "testqueuename";

        QueueClient sut;

        @BeforeEach
        public void init() {
            sut = new QueueClientBuilder()
                    .connectionString(AZURE_STORAGE_CONTAINER.getConnectionString())
                    .queueName(TEST_QUEUE_NAME)
                    .buildClient();

            sut.create();
        }

        @AfterEach
        public void cleanup() {
            sut.delete();
        }

        @Test
        public void givenSomeTestData_whenSendingItToTheQueueAndRetrievingIt_objectsShouldBeEqual() {
            ExampleDto sentObject = new ExampleDto("Tom was here ;)");

            sut.sendMessage(BinaryData.fromObject(sentObject));
            QueueMessageItem receivedMessage = sut.receiveMessage();
            ExampleDto receivedObject = receivedMessage.getBody().toObject(ExampleDto.class);

            assertThat(receivedObject).isEqualTo(sentObject);
        }

        @Test
        public void givenSomeTestData_whenSendingItToTheQueueAndRetrievingIt_itStillExistsIntheQueue() {
            ExampleDto sentObject = new ExampleDto("Tom was here ;)");

            sut.sendMessage(BinaryData.fromObject(sentObject));
            sut.receiveMessage();

            // The queue is not like an event-bus where messages are automatically consumed ;) the message is still there.
            assertThat(sut.getProperties().getApproximateMessagesCount()).isEqualTo(1);
        }
    }
}
