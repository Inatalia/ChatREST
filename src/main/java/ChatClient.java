import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.jboss.resteasy.plugins.providers.multipart.OutputPart;

import javax.ws.rs.FormParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * @author Irene Hardjono
 * @version $Revision: 1 $
 */
public class ChatClient
{
   public static void main(String[] args) throws Exception
   {
      String name = args[0];

      System.out.println();
      System.out.println();
      System.out.println();
      System.out.println();

      final Client client = new ResteasyClientBuilder()
                          .connectionPoolSize(3)
                          .build();
      WebTarget target = client.target("http://localhost:8080/services/chat");

      target.request().async().get(new InvocationCallback<Response>()
      {
         @Override
         public void completed(Response response)
         {
            Link next = response.getLink("next");
            String message = response.readEntity(String.class);
            System.out.println();
            System.out.print(message);// + "\r");
            System.out.println();
            System.out.print("> ");
            client.target(next).request().async().get(this);
         }

         @Override
         public void failed(Throwable throwable)
         {
            System.err.println("FAILURE!");
         }
      });


      while (true)
      {
         System.out.print("> ");
         BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
         String message = br.readLine();
         if(message.toLowerCase().contains("upload")){
        	 String filename = message.split(" ")[1];
        	 File file = new File(filename);
        	 if(filename.isEmpty()){
        		 target.request().post(Entity.text(name + ": No filename specified!"));
        	 }
        	 else if(!file.exists()){
        		 target.request().post(Entity.text(name + ": Invalid filename!"));
        	 }
        	 else if(file.isFile()){
			MultipartFormDataOutput mdo = new MultipartFormDataOutput();
        	       	mdo.addFormData("file1", new FileInputStream(file), 
        	    		   MediaType.APPLICATION_OCTET_STREAM_TYPE);
        		GenericEntity<MultipartFormDataOutput> entity = new GenericEntity<MultipartFormDataOutput>(mdo) {};
        		Response r = target.request().post(Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE));

        		/*MultipartFormDataInput  input =  r.readEntity(MultipartFormDataInput.class);
                 	Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
      
                 	//Get file data to save to disk
                 	List<InputPart> inputParts = uploadForm.get("file2");
                 	for (InputPart inputPart : inputParts)
                 	{
                		try
                     		{
                			InputStream inputStream = inputPart.getBody(InputStream.class, null);
                		 	OutputStream out = new FileOutputStream(new File("/Users/irenenatalia/Desktop/" + file.getName()));
                		 	int read = 0;
                         		byte[] bytes = new byte[2048];
                         		while ((read = inputStream.read(bytes)) != -1) {
                        	 		out.write(bytes, 0, read);
                         		}
                         		inputStream.close();
                         		out.flush();
                         		out.close();
                     		} catch (Exception e) {
                         		e.printStackTrace();
                     		}
                 	}*/
                 	target.request().post(Entity.text(name + ": sending " + file.getName()));
        	 }
         } 
         else{
     	 	target.request().post(Entity.text(name + ": " + message));
      	 }
      }
   }
}
