package com.segal.mongorest.core.service;

import com.segal.mongorest.core.pojo.BaseDocument;
import com.segal.mongorest.core.util.BeanValidator;
import com.segal.mongorest.core.util.DefaultTimeProvider;
import com.segal.mongorest.core.util.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 4/15/14
 * Time: 5:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultMongoCrudService<T extends BaseDocument> implements CrudService<T> {

	protected Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	protected BeanValidator beanValidator;

	protected PersistenceListenerManager<T> persistenceListenerManager = new PersistenceListenerManager<>();
	protected CrudRepository<T, String> crudRepository;
	protected TimeProvider timeProvider = new DefaultTimeProvider();

	public DefaultMongoCrudService() {
	}

	public DefaultMongoCrudService(CrudRepository<T, String> crudRepository) {
		this(crudRepository, null);
	}

	public DefaultMongoCrudService(CrudRepository<T, String> crudRepository, TimeProvider timeProvider) {
		this.crudRepository = crudRepository;
		if (timeProvider == null) this.timeProvider = new DefaultTimeProvider();
		else this.timeProvider = timeProvider;
	}

	public T update(T pojo) {
		return doSave(pojo, true);
	}

	public Collection<T> update(Collection<T> pojos) {
		return doSave(pojos, true);
	}

	public T create(T pojo) {
		return doSave(pojo, false);
	}

	public Collection<T> create(Collection<T> pojos) {
		return doSave(pojos, false);
	}

	protected T doSave(T pojo, boolean isUpdate) {
		// Validate default constraints
		Set<ConstraintViolation<T>> violations = beanValidator.validate(pojo);

		if (isUpdate) {
			// Validate update constraints
			violations.addAll(beanValidator.validate(pojo, UpdateChecks.class));
		} else {
			// Validate create constraints
			violations.addAll(beanValidator.validate(pojo, CreateChecks.class));
		}

		if (violations.isEmpty()) {

			long timestamp = timeProvider.getSystemTimeMillis();

			// Add createdMillis for create operations
			if (!isUpdate) {
				pojo.setCreatedMillis(timestamp);
			}

			// Add lastUpdatedMillis for all save operations
			pojo.setLastUpdatedMillis(timestamp);
			T saved = crudRepository.save(pojo);
			if (isUpdate) {
				persistenceListenerManager.notifyDocumentUpdated(saved);
			} else {
				persistenceListenerManager.notifyDocumentAdded(saved);
			}
			return saved;
		} else {
			String message = "";
			for (ConstraintViolation<T> violation : violations) {
				message += violation.getPropertyPath() + " " + violation.getMessage() + ", ";
			}
			throw new ConstraintViolationException(message, new HashSet<ConstraintViolation<?>>(violations));
		}
	}

	protected Collection<T> doSave(Collection<T> pojos, boolean isUpdate) {
		List<T> saved = new ArrayList<T>();
		for (T pojo : pojos) {
			saved.add(doSave(pojo, isUpdate));
		}
		return saved;
	}

	public void delete(String id) {
		if (id == null) {
			throw new IllegalArgumentException("Parameter id cannot be null.");
		}
		crudRepository.delete(id);
		persistenceListenerManager.notifyDocumentDeleted(id);
	}

	public void deleteAll() {
		crudRepository.deleteAll();
	}

	public T findOne(String id) {
		if (id == null) {
			throw new IllegalArgumentException("Parameter id cannot be null.");
		}
		return crudRepository.findOne(id);
	}

	public Iterable<T> findAll() {
		return crudRepository.findAll();
	}

	public void setCrudRepository(CrudRepository<T, String> crudRepository) {
		this.crudRepository = crudRepository;
	}

	public void setTimeProvider(TimeProvider timeProvider) {
		this.timeProvider = timeProvider;
	}

	public CrudRepository<T, String> getCrudRepository() {
		return crudRepository;
	}

	public TimeProvider getTimeProvider() {
		return timeProvider;
	}

	public PersistenceListenerManager<T> getPersistenceListenerManager() {
		return persistenceListenerManager;
	}

}
