package com.segal.mongorest.core;

import com.google.common.reflect.TypeToken;
import com.segal.mongorest.core.pojo.BaseDocument;
import com.segal.mongorest.core.service.CrudService;
import com.segal.mongorest.core.service.PersistenceListener;
import com.segal.mongorest.core.support.*;
import com.segal.mongorest.core.util.ApplicationRegistry;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;

import javax.annotation.PostConstruct;
import java.lang.reflect.Type;

//import static org.easymock.EasyMock.*;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 4/17/14
 * Time: 11:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class DocumentValidationTest<T extends BaseDocument> extends EasyMockSupport {

	Logger log = LoggerFactory.getLogger(this.getClass());
	String mockId = "NOT_NULL";

	protected CrudRepository<T, String> repository;
	protected CrudService<T> service;
	protected DocumentProvider<T> documentProvider;

	@Autowired
	protected ApplicationRegistry applicationRegistry;

	@Autowired
	protected TestRegistry testRegistry;

	private final TypeToken<T> typeToken = new TypeToken<T>(getClass()) { };
  private final Type type = typeToken.getType();

	public DocumentValidationTest() {
	}

	@PostConstruct
	public void init() {
		String documentType = applicationRegistry.getDocumentType((Class) type);
		this.repository = (CrudRepository<T, String>) applicationRegistry.getCrudRepository((Class) type);
		this.service = (CrudService<T>) applicationRegistry.getCrudService((Class) type);
		this.documentProvider = (DocumentProvider<T>) testRegistry.getDocumentBuilder(documentType);

		log.info("Initializing TimeProvider mocks...");
		EasyMock.resetToNice(service.getTimeProvider());
		EasyMock.expect(service.getTimeProvider().getSystemTimeMillis()).andStubReturn(1000L);
	}

	@Rule
	public ExpectedException rule = ExpectedException.none();

	@Test
	public void testDocuments() {
		for (DocumentTestResult<T> result : documentProvider.createDocuments()) {
			log.info("Validating: " + result);
			if (result instanceof ValidDocumentTestResult) {
				if (DocumentTestResult.Operation.create.equals(result.getOperation()) ||
						DocumentTestResult.Operation.update.equals(result.getOperation())) {
					testValidSave((ValidDocumentTestResult<T>) result);
				} else if (DocumentTestResult.Operation.find.equals(result.getOperation())) {
					testValidFind((ValidDocumentTestResult<T>) result);
				} else throw new IllegalArgumentException("Unexpected Operation type: " + result.getOperation());
			} else if (result instanceof InvalidDocumentTestResult) {
				if (DocumentTestResult.Operation.create.equals(result.getOperation()) ||
						DocumentTestResult.Operation.update.equals(result.getOperation())) {
					try {
						testInvalidSave((InvalidDocumentTestResult<T>) result);
					} catch (Exception e) {
						log.info("Received expected exception: " + e.getMessage());
					}
				} else if (DocumentTestResult.Operation.find.equals(result.getOperation())) {
					try {
						testInvalidFind((InvalidDocumentTestResult<T>) result);
					} catch (Exception e) {
						log.info("Received expected exception: " + e.getMessage());
					}
				} else throw new IllegalArgumentException("Unexpected Operation of type: " + result.getOperation());
			} else throw new IllegalArgumentException("Unexpected DocumentTestResult of type: " + result.getClass());
		}
	}

	public void testValidFind(ValidDocumentTestResult<T> result) {
		EasyMock.resetToNice(repository);

		// This is the real test - want to make sure that save actually gets called for input that should pass validation
		EasyMock.expect(repository.findOne((result.getDocument().getId()))).andReturn(result.getDocument());
		EasyMock.replay(repository);
		T document = service.findOne(result.getDocument().getId());
		EasyMock.verify(repository);
	}

	public void testInvalidFind(InvalidDocumentTestResult<T> result) {
		rule = ExpectedException.none();
		rule.expect(result.getExceptionClass());
		T document = service.findOne(result.getDocument().getId());
	}

	public void testInvalidSave(InvalidDocumentTestResult<T> result) throws Exception {
		rule = ExpectedException.none();
		rule.expect(result.getExceptionClass());
		service.create((T) result.getDocument());
	}

	public void testValidSave(ValidDocumentTestResult<T> result) {
		PersistenceListener<T> mockPersistenceListener = createNiceMock(PersistenceListener.class);
		service.getPersistenceListenerManager().addPersistenceListener(mockPersistenceListener);

		if (DocumentTestResult.Operation.update.equals(result.getOperation())) {
			result.getDocument().setId(mockId);
			mockPersistenceListener.documentUpdated(result.getDocument());
		} else {
			mockPersistenceListener.documentAdded(result.getDocument());
		}
		EasyMock.resetToNice(repository, service.getTimeProvider());
		// This is the real test - want to make sure that save actually gets called for input that should pass validation
		EasyMock.expect(repository.save((result.getDocument()))).andReturn(result.getDocument());
		EasyMock.replay(repository, mockPersistenceListener);

		if (DocumentTestResult.Operation.update.equals(result.getOperation())) {
			service.update(result.getDocument());
		} else {
			service.create(result.getDocument());
		}
		EasyMock.verify(mockPersistenceListener);

		service.getPersistenceListenerManager().removePersistenceListener(mockPersistenceListener);
	}

	@Test
	public void testDelete() {
		log.info("Attempting to delete ID '" + mockId + "'...");
		doTestDelete(mockId);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidDelete() {
		log.info("Attempting to delete a null ID...");
		doTestDelete(null);
	}

	private void doTestDelete(String id) {
		service.delete(id);
		PersistenceListener<T> mockPersistenceListener = createNiceMock(PersistenceListener.class);
		service.getPersistenceListenerManager().addPersistenceListener(mockPersistenceListener);
		mockPersistenceListener.documentDeleted(id);
		EasyMock.replay(mockPersistenceListener);
		service.delete(id);
		EasyMock.verify(mockPersistenceListener);
		service.getPersistenceListenerManager().removePersistenceListener(mockPersistenceListener);
	}

	public void setRepository(CrudRepository repository) {
		this.repository = repository;
	}

	public void setService(CrudService<T> service) {
		this.service = service;
	}

	public void setDocumentProvider(DocumentProvider<T> documentProvider) {
		this.documentProvider = documentProvider;
	}

}
