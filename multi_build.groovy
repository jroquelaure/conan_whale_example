import groovy.json.JsonSlurperClassic

def get_conan_images()
{

    def aqlCmd = "curl -uadmin:password -X POST http://localhost:8081/artifactory/api/search/aql -T /Users/jon/workspace/conan/docker-multibuild/conan_whale_example/getConanBuilderImages.aql"
    def imagesPath = aqlCmd.execute().text
    def jsonSlurper = new JsonSlurperClassic()
    
    def images = jsonSlurper.parseText("${imagesPath}")
     
   
    return images.results
}

def launch_task_group(imageNames, conf_repo_branch, conf_repo_url,user_channel,name,version)
{
    tasks = [:]
    for(int i=1; i<imageNames.path.size(); i++){
             def imageName = imageNames.path[i]
             sh "echo $imageName"
             tasks[imageName] = { -> build(job: "conan-multi-task2",
                              parameters: [
                                 string(name: "image", value: imageName),
                                 string(name: "name", value: name),
                                 string(name: "version", value: version),
                                 string(name: "user_channel", value: user_channel),
                                 string(name: "conf_repo_url", value: conf_repo_url),
                                 string(name: "conf_repo_branch", value: conf_repo_branch)
                              ]
                       )
            }
        }
       
            
        parallel(tasks)
}

properties([parameters([string(description: 'Name', name: 'name', defaultValue: 'LIB_A'),
                        string(description: 'Version', name: 'version', defaultValue: '1.0'),
                        string(description: 'User/Channel', name: 'user_channel', defaultValue: 'lasote/stable'),
                        string(description: 'Config repository URL', name: 'conf_repo_url', defaultValue: 'https://github.com/jroquelaure/skynet_example.git'),
                        string(description: 'Config repository branch', name: 'conf_repo_branch', defaultValue: 'master')
                        ])])

@NonCPS def entries(m) {m.collect {k, v -> [k, v]}}

node{
    def imageNames
    def name_version = params.name + "/" + params.version
    
    stage("Configure repositories")
    {
        def server = Artifactory.server ("artifactory_local")
    }
    stage("Get target environment list")
    {
        //list available conan docker images from artifactory
        //=> AQL
        imageNames = get_conan_images()
        sh "echo $imageNames"
    }
    stage("Run builds in parallel for environment")
    {
        //foreach docker image for conan from RTF build job (current docker image) _
        //first step simple build
        tasks = [:]
        //imageNames = get_conan_images()
        launch_task_group(imageNames,conf_repo_branch, conf_repo_url,user_channel,name,version)
        
        
    }
}