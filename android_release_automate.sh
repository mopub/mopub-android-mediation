#!/usr/bin/env bash

# ==================================================== #

### Set up gitsubmodules to run gradle build ###
git submodule update --recursive --remote
#git add submodules https://github.com/mopub/mopub-android-sdk.git
#git add submodules https://github.com/mopub/mopub-android.git
#git add submodules https://github.com/mopub/mopub-pso-tools.git

#export FIREBASE_TOKEN=1/WJzZgUE0m8tGH1skDJtKroN93NoTFUP4cDnR3GDzqItm1--fc-huQSkYXJA7IFpf
USER_NAME=dravyan
API_KEY=913e26a591eaaefc7447efb002bbb67f179a87a2

# Networks this script checks for
NETWORKS=( 
    AdColony
    AdMob
    AppLovin 
    Chartboost
    FacebookAudienceNetwork
    Flurry 
    IronSource
    OnebyAOL
    Tapjoy
    UnityAds
    Verizon
    Vungle
)

### Function to get display name for Firebase update ###
function get_display_name {
    key=$1
    out=$2
    name=$key
    case "$key" in
        AdMob ) name="Google (AdMob)";;
        FacebookAudienceNetwork ) name="Facebook Audience Network";;
        Flurry ) name="Yahoo! Flurry";;
        IronSource ) name="ironSource";;
        OnebyAOL ) name="One by AOL";;
        UnityAds ) name="Unity Ads";;
    esac
    eval "$out='$name'"
}

### Function to read Adapter version from AdapterConfiguration ###
function read_networkAdapter_version
{
 versionnumber=`grep -r "project.version = " ./$1/ | awk '{print $3}' | sed s/\'//g | sed s/\;//g`
 echo $versionnumber
 sdkverion=`echo $versionnumber | cut -c1-5`
 echo $sdkverion
 lowercaseselection=$(echo "$1" | tr '[:upper:]' '[:lower:]')
 mv ./libs/mopub-$lowercaseselection-adapters-*.aar ./libs/$lowercaseselection-$versionnumber.aar
 
 ### Generate pom file for adapter version ###
echo '<?xml version="1.0" encoding="UTF-8"?>
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.mopub.mediation</groupId>
  <artifactId>'"$lowercaseselection"'</artifactId>
  <version>'"$versionnumber"'</version>
  <packaging>aar</packaging>
</project>' >> ./libs/sample.pom
  echo $lowercaseselection
  echo $versionnumber
  mv ./libs/*.pom ./libs/$lowercaseselection-$versionnumber.pom

  ### CREATING TAG RELEASE IN GITHUB ###
  commitId= `git rev-parse HEAD`
  echo $commitId
  tagname="$lowercaseselection-$versionnumber"
  echo $tagname
  #git tag -a $lowercaseselection-$versionnumber -m "new tag" $commitId
  #git push origin $versionnumber

  ### Publish release in Github [TO_DO]
  #curl -H "Authorization: token 6a8dbab0927c08c22700c46491d477e2ca1da73f" --data '{"tag_name": "$lowercaseselection-$versionnumber","target_commitish": "$commitId","name": "$versionnumber","body": "Release of version $versionnumber. Refer https://github.com/mopub/mopub-android-mediation/blob/master/$1/CHANGELOG.md.", "draft": false, "prerelease": false}' https://api.github.com/repos/mopub/mopub-android-mediation/releases

  curl -H "Authorization: token 6a8dbab0927c08c22700c46491d477e2ca1da73f" --data '{"tag_name": "'"$tagname"'","target_commitish": "'"$commitId"'","name": "'"$versionnumber"'","body": "Refer https://github.com/mopub/mopub-android-mediation/blob/master/$1/CHANGELOG.md.","draft": false,"prerelease": false}' https://api.github.com/repos/mopub/mopub-android-mediation/releases

  ### RELEASING aar AND pom TO BINTRAY ###
  ##curl -T <FILE.EXT> -udravyan:<API_KEY> https://api.bintray.com/content/mopub/mopub-android-mediation/<YOUR_COOL_PACKAGE_NAME>/<VERSION_NAME>/<FILE_TARGET_PATH>
  curl -T ./libs/$lowercaseselection-$versionnumber.aar -u$USER_NAME:$API_KEY https://api.bintray.com/content/mopub/mopub-android-mediation/com.mopub.mediation.$lowercaseselection/$versionnumber/com/mopub/mediation/$lowercaseselection/$versionnumber/
  curl -T ./libs/$lowercaseselection-$versionnumber.pom -u$USER_NAME:$API_KEY https://api.bintray.com/content/mopub/mopub-android-mediation/com.mopub.mediation.$lowercaseselection/$versionnumber/com/mopub/mediation/$lowercaseselection/$versionnumber/
  if [ $? -eq 0 ]; then
    ### UPDATE FIREBASE ###
    echo "Updating firebase JSON..."
    firebase_project="mopub-mediation"
    FIREBASE_TOKEN="1/WJzZgUE0m8tGH1skDJtKroN93NoTFUP4cDnR3GDzqItm1--fc-huQSkYXJA7IFpf"
    get_display_name $i name
    json_path="/releaseInfo/$name/Android/version"

    echo $i
    echo $versionnumber
    if [ -z "$FIREBASE_TOKEN" ]; then
        print_red_line "\$FIREBASE_TOKEN environment variable not set!"
    else
        firebase database:set --confirm "/releaseInfo/$name/Android/version/adapter/" --data "\"$versionnumber\"" --project $firebase_project --token $FIREBASE_TOKEN
        firebase database:set --confirm "/releaseInfo/$name/Android/version/sdk/" --data "\"$sdkverion\"" --project $firebase_project --token $FIREBASE_TOKEN
        if [[ $? -ne 0 ]]; then
            echo "ERROR: Failed to run firebase commands; please follow instructions at: https://firebase.google.com/docs/cli/"
        else
            echo "Done updating firebase JSON"
        fi
      fi
else
    echo Failed to Push to bintray. Please update bintray before updating Firebase.
fi

### CLEAN UP /LIBS FOLDER ###
 rm -r ./libs/*.pom

}

for i in "${NETWORKS[@]}"
do
    changed=$(git log --name-status -1 --oneline ./ | grep $i)
    if [[ ! -z "$changed" ]]; then
        echo "$changed"
        read_networkAdapter_version  $i
    fi  
done


#printf "\nWhich network do you want to build jar for?\n"
#select opt in "${NETWORKS_TO_EXPORT[@]}" All; do
#   SELECTED_NETWORK="$opt"
#   case "$SELECTED_NETWORK" in
#        "" ) echo "Invalid option. Try another one."
#             continue;;
#        All | Missing )
 #            break;;
#        * )  NETWORKS_TO_EXPORT=( $opt )
#             break;;
#    esac
#    break
#done
#printf "\nSelected network: $SELECTED_NETWORK\n"

    
#read_networkAdapter_version  $SELECTED_NETWORK

#if [ "$SELECTED_NETWORK" = "All" ]
#then
#for i in "${NETWORKS_TO_EXPORT[@]}" 
#do
  # : 
   #read_networkAdapter_version  $i
#done
#fi

