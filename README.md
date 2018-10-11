## Migration Station
Using the Fusion Admin makes quick work of complex tasks  like the fields manager, creating datasources, and using the Index and Query workbenches. However, you often need to move your fusion objects to other servers, and there is no easy way to do this out of the box. There is also no way to apply version control to these.

One way to make changes is to use a REST tool like Postman:
1. download a change from the server, 
2. save as a text file, 
3. then upload to another server using Fusions REST API.

This is a manual process that is tedious, but can be setup as a script.

###Better Way
We have developed a process that enables us to use development best practices such as version control, and continuous integration on our Fusion configurations.

It is a command line interface for downloading and uploading Fusion objects and collections between servers in order to facilitate development, testing , and versioning of Fusion objects (datasources, query and index pipelines, schedules, jobs, profiles, apps, etc) and SOLR collection configurations.
This is implemented as a java program using the Fusion REST API, and can be easily run from your Continuous Integration service to promote code from your source control repository. 

Version 4 of Fusion now has a facility to import and export an App into a zip file. The above process is different it enables you to do several things:
* Use version control on your SOLR/Fusion configurations
* Fine-grain control -  select specific objects to download or upload. For example, you can use the CLI to move a single datasource along with a query pipeline-  move the “jira-devwork” datasource, along with the “jira-main” query pipeline from a git repo to a running fusion server.
* Release management – using CI tools, we are able to push a release to a server, and also roll it back if there are problems.
