import groovy.json.JsonSlurperClassic

def get_conan_images()
{
    

    def aqlCmd = "curl -uadmin:password -X POST http://localhost:8081/artifactory/api/search/aql -T /Users/jon/workspace/conan/docker-multibuild/conan-demo/getConanBuilderImages.aql"
    def imagesPath = aqlCmd.execute().text
    def jsonSlurper = new JsonSlurperClassic()
    def images = jsonSlurper.parseText("${imagesPath}")

    return images.results
}


node{
    def imageNames
    stage("Configure repositories")
    {
        def server = Artifactory.server ("artifactory_local")
    }
    stage("Get target environment list")
    {
        //list available conan docker images from artifactory
        //=> AQL
        imageNames = get_conan_images()

    }
    stage("Run builds in parallel for environment")
    {
        //foreach docker image for conan from RTF build job (current docker image) _
        tasks = [:]
        imageNames = get_conan_images()
        for(int i=0; i<imageNames.path.size(); i++){
             image = imageNames.path[i]
             sh "echo $image"
             tasks[image] = { -> build(job: "conan-single-test",
                              parameters: [
                                 string(name: "image", value: image)
                              ]
                       )
            }
        }
        parallel(tasks)
        //final the multi-build -> parrallel per profiles per environment
        //Need to link conan profile to docker image with metadata in order to execute build order command for each profiles
        
        //then parrallelize per image -> build order workflow
        
        //Warn : final need merge of PR from nico (dir with docker image in jenkins does not work)

    }
}