package com.segal.mongorest.example.rest;

import com.segal.mongorest.core.service.CrudService;
import com.segal.mongorest.core.support.DocumentProvider;
import com.segal.mongorest.example.ExampleMockApplicationConfigExample;
import com.segal.mongorest.example.ExampleMockRestConfig;
import com.segal.mongorest.example.pojo.Book;
import com.segal.mongorest.web.DocumentControllerTest;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 4/24/14
 * Time: 12:54 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {ExampleMockApplicationConfigExample.class, ExampleMockRestConfig.class})
public class BookControllerTest extends DocumentControllerTest<Book> {

	@Override
	@Autowired
	@Qualifier("bookControllerDocumentProvider")
	public void setDocumentProvider(DocumentProvider<Book> documentProvider) {
		super.setDocumentProvider(documentProvider);
	}

	@Override
	@Autowired
	@Qualifier("bookService")
	public void setCrudService(CrudService<Book> crudService) {
		super.setCrudService(crudService);
	}

}
