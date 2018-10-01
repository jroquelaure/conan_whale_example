properties([parameters([string(description: 'Build label', name: 'build_label', defaultValue: 'Unamed'),
                        string(description: 'Channel', name: 'channel', defaultValue: 'stable'),
                        string(description: 'Name', name: 'name', defaultValue: 'LIB_A'),
                        string(description: 'Version', name: 'version', defaultValue: '1.0'),
                        string(description: 'Conan agent docker image name', name: 'image', defaultValue: 'conan/conangcc7'),
                        string(description: 'Profile', name: 'profile', defaultValue: './profiles/64bits'),
                        string(description: 'Config repository branch', name: 'conf_repo_branch', defaultValue: 'master'),
                        string(description: 'Config repository url', name: 'conf_repo_url', defaultValue: 'https://github.com/jroquelaure/skynet_example.git'),
                       ])])

node {
    docker.withTool("default"){
    docker.image("localhost:8081/docker-prod-local/"+image).inside('-v /tmp/:/tmp/  --net=host') { 
    
    currentBuild.displayName = params.build_label
    def buildInfo
    def data
    def conf_repo_dir
    def client
    def serverName
    def name_version = params.name + "/" + params.version
    def serverDevName
            def server
        stage("Configure/Get repositories"){

            
                git branch: params.conf_repo_branch, url: params.conf_repo_url
                data = readYaml file: "conan_ci_conf.yml"
                conf_repo_dir = pwd()
            
            server = Artifactory.server data.artifactory.name
            client = Artifactory.newConanClient()
            
            serverName = client.remote.add server: server, repo: "conan-prod-local"
            
            //client.run(command: "remote remove conan.io")
            
        }
        stage("Build packages"){
           
                    client.run(command: "create ./"+data.repos[name_version].dir+"/. lasote/stable -pr \"" + conf_repo_dir + "/" + params.profile + "\"")
                
            
        }
        
        stage("Upload Artifactory"){
            serverDevName = client.remote.add server: server, repo: "conan-dev-local"
            String command = "upload ${name_version}@lasote/stable -r ${serverDevName} --all -c"
            buildInfo = client.run(command: command)
            buildInfo.env.collect()
            server.publishBuildInfo buildInfo
        }
        
        stage("Test"){
               
        }
        
        stage("Promote"){
            def promotionConfig = [
            // Mandatory parameters
            'buildName'          : buildInfo.name,
            'buildNumber'        : buildInfo.number,
            'targetRepo'         : 'conan-prod-local',
    
            // Optional parameters
            'comment'            : 'ready for prod',
            'sourceRepo'         : 'conan-dev-local',
            'status'             : 'Released',
            'includeDependencies': false,
            'copy'               : false
        ]

        // Promote build
        server.promote promotionConfig   
        
        }
    }
    }
    
}