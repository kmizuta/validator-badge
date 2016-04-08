package io.swagger.validator.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingMessage;

import io.swagger.parser.util.SwaggerDeserializationResult;
import io.swagger.validator.models.SchemaValidationError;
import io.swagger.validator.models.ValidationResponse;

public class OraValidatorService extends ValidatorService {

	@Override
	protected String getSchema() throws Exception {
		// TODO (Later) Need to define Oracle hyper-schema and use that instead of standard Swagger schema
		return super.getSchema();
	}

	@Override
	protected SwaggerDeserializationResult readSwagger(String content) throws IllegalArgumentException {
		// TODO (Later) Need to read into an Oracle object.
		return super.readSwagger(content);
	}
	
	public void validateByApi(HttpServletRequest request, HttpServletResponse response, String product, String api, String level) {
        LOGGER.info("validation product: " + product + ", api: " + api + ", level: " + level + ", forClient: " + getRemoteAddr(request));

        ValidationResponse payload = null;

        try {
            payload = debugByApi(product, api, level);
        }
        catch (Exception e) {
            error(response);
        }

        if(payload == null) {
            fail(response);
            return;
        }
        if(payload.getMessages() == null && payload.getSchemaValidationMessages() == null) {
            success(response);
        }

        if(payload.getSchemaValidationMessages() != null) {
            for(SchemaValidationError message : payload.getSchemaValidationMessages()) {
                if(INVALID_VERSION.equals(message)) {
                    upgrade(response);
                }
            }
        }

        error(response);		
	}
	
	public ValidationResponse debugByApi(String product, String api, String level) throws Exception {
		ValidationResponse output = new ValidationResponse();
		
		List<Category> categories;
		try {
			categories = getCategories(product, api, level);
		} catch (java.lang.IllegalArgumentException e) {
			return null;
		} catch (Exception e) {
            ProcessingMessage pm = new ProcessingMessage();
            pm.setLogLevel(LogLevel.ERROR);
            pm.setMessage(e.getMessage());
            output.addValidationMessage(new SchemaValidationError(pm.asJson()));
            return output;
		}
		
		Map<Category, ValidationResponse> responses = new HashMap<Category, ValidationResponse>();
		List<Thread> threads = new ArrayList<Thread>();
		for (Category category : categories) {
			Fetcher fetcher = new Fetcher(category, responses);
			
			Thread thread = new Thread(fetcher);
			thread.start();
			threads.add(thread);
		}
			
		for (Thread t : threads) { t.join(); }
		
		for (Category category : responses.keySet()) {
			ValidationResponse categoryOutput = responses.get(category);
			
			// Prepend the category name to the messages and add to output.
			List<String> messages = categoryOutput.getMessages();
			if (messages != null) {
				for (String message : messages) {
					StringBuilder buf = new StringBuilder(category.name).append(": ").append(message);
					output.addMessage(buf.toString());
				}
			}
			List<SchemaValidationError> errors = categoryOutput.getSchemaValidationMessages();
			if (errors != null) {
				for (SchemaValidationError sve : errors) {
					String message = sve.getMessage();
					StringBuilder buf = new StringBuilder(category.name).append(": ").append(message);
					sve.setMessage(buf.toString());
					output.addValidationMessage(sve);
				}
			}
		}
		
		return output;
	}
	
	private List<Category> getCategories(String product, String api, String level) throws Exception {
		List<Category> categories = new ArrayList<Category>();
		
		String url = new StringBuilder("http://ohcrest.doceng.oraclecorp.com/api/v1/products/")
				.append(product)
				.append("/levels/").append(level)
				.append("/apis/").append(api)
				.toString();
		String apiContent;
		
        try {
        	apiContent = getUrlContents(url);
        } catch (IOException e) {
        	throw new IllegalArgumentException("Can't read APIs from " + url);
        }
        JsonNode apiNode = readNode(apiContent);
        JsonNode resourceNode = apiNode.get("resource");
        Iterator<JsonNode> iter = resourceNode.iterator();
        while (iter.hasNext()) {
            JsonNode categoryNode = iter.next();        	
            String name = categoryNode.get("name").asText();
            String title = categoryNode.get("title").asText();
            String link = new StringBuilder("http://ohcrest.doceng.oraclecorp.com")
            		.append(categoryNode.get("link").get("href").asText())
            		.toString();
            
            Category category = new Category(name, title, link);
            categories.add(category);
        }

		return categories;
	}

	private class Category {
		String name, title, url;
		
		Category(String name, String title, String url) {
			this.name = name;
			this.title = title;
			this.url = url;
		}
	}
	
	private class Fetcher implements Runnable {

		Category category;
		Map<Category, ValidationResponse> responses;
		
		Fetcher(Category category, Map<Category, ValidationResponse> responses) {
			this.category = category;
			this.responses = responses;
		}
		
		@Override
		public void run() {
			try {
				LOGGER.info("Trying " + category.url);
				ValidationResponse categoryOutput =  debugByUrl(category.url);
				LOGGER.info("Got " + category.url);
				responses.put(category, categoryOutput);
			} catch(Exception e) {
				LOGGER.error("Error " + category.url);
	            ProcessingMessage pm = new ProcessingMessage();
	            pm.setLogLevel(LogLevel.ERROR);
	            pm.setMessage(e.getMessage());
	            ValidationResponse output = new ValidationResponse();
	            output.addValidationMessage(new SchemaValidationError(pm.asJson()));
	            responses.put(category, output);
			}
		}
		
	}
	public static void main(String[] args) throws Exception {
		OraValidatorService service = new OraValidatorService();
		ValidationResponse response = service.debugByApi("fusionapps", "sales-r11", "dev");
		
		List<String> messages = response.getMessages();
		if (messages != null) {
			for (String message : messages) {
				System.out.println(message);
			}
		}
		List<SchemaValidationError> errors = response.getSchemaValidationMessages();
		if (errors != null) {
			for (SchemaValidationError sve : errors) {
				String message = sve.getMessage();
				System.out.println(message);
			}
		}

		System.out.println("Done");
	}
}
