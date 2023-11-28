<?php
/**
 * @author Christoph Steisslinger christoph.steisslinger@writeme.com
 * @version 1.2.0
 * @since 17.08.2018
 */
//start seasion 
ob_start();
	ini_set('session.cache_limiter','public');
	session_cache_limiter(false);
	header('Cache-Control: max-age=900');
	session_start();
	if(isset($_SESSION["DB"])){
		$user = $_SESSION["username"];
		$pwd = $_SESSION["pwd"];
		$db = @mysqli_connect("10.10.89.80:3306", "$user", "$pwd","inhousedatabase");
	}else{
		header ("Location: Patindex.php");
	}		
	//upload xml to webserver
$ds          = DIRECTORY_SEPARATOR;  
$storeFolder = '';   
if (!empty($_FILES)) {   
    $tempFile = $_FILES['file']['tmp_name'];                     
    $targetPath = dirname( __FILE__ ) . $ds. $storeFolder . $ds;  
    $targetFile =  $targetPath. $_FILES['file']['name'];  
    move_uploaded_file($tempFile,$targetFile); 
	$filepath=$targetFile;
	//does file exist?
if (! file_exists($filepath)) {
    echo('xml file not found');
} else { 
           $pyro = simplexml_load_file($filepath);  
			unlink($filepath);
}
//start
// pepare statement
if (! ($sql = $db->prepare("insert into pyroergebnis (chromosom,result,rsname, note) values (?,?,?,?)"))) {
    echo 'prepare error' . $db->error;
}
if (! ($sql->bind_param("ssss", $test, $result, $testname, $note))) {
    echo "bind error" . $sql->error;
}
/**
 * for each samplerun-node aka.
 * every used well
 */
foreach ($pyro->platerun->sampleruns->samplerun as $wea) {
    // only used wells are interesting that have a snp position in the note-node
    if (dom_import_simplexml($wea->used)->nodeValue == "True" && is_numeric(dom_import_simplexml($wea->note)->nodeValue)) {        
        /**
         * example
         * 0001-17 --> 0001/17
         */
        $sampleid = dom_import_simplexml($wea->{'sample-text'})->nodeValue;
                /**
                 * connys special cases
                 */
        if (strlen($sampleid)==4){
            $sampleid="000".$sampleid;
        }
        if(strlen($sampleid)==5){
            $sampleid="00".$sampleid;
        }
        if(strlen($sampleid)==6){
            $sampleid="0".$sampleid;
        }
        //get journal_number
        $sampleid = str_replace("-", "/", $sampleid);
        $numnum=strpos($sampleid,"/");
        /**
         * extract info from xml file
         * @var Ambiguous $chr
         */
        $chr = dom_import_simplexml($wea->{'entry-prototype'}->name)->nodeValue;
        $test = substr($chr, strpos($chr, "--") + 2);
        $test = substr($test, strpos($test, ".") + 1);
        // chr1 hat leertaste vorangestellt deshalb trim
        $test = trim($test);
        $result = dom_import_simplexml($wea->{'analysis-results'}->{'snp-analysis-result'}->results->template->genotype->result)->nodeValue;
        $name = dom_import_simplexml($wea->{'entry-prototype'}->name)->nodeValue;
        $testname = substr($name, 0, strpos($chr, " --"));
        $note = dom_import_simplexml($wea->note)->nodeValue;
        $quality=dom_import_simplexml($wea->{'analysis-results'}->{'snp-analysis-result'}->results->template->genotype->quality)->nodeValue;
        
        // selectr possible duplicates. creates lots of traffic
        if (! $sqlgetduplicate = $db->query("select SNPID from pyroergebnis py where py.note='$note' AND py.result='$result'")) {
            echo "exec of sqlgetduplicate s failed" . $sqlgetduplicate->error;
        }
        //is patient already in db?
        if (! $jndup= $db->query("select journal_number from sample_pyro sp where sp.journal_number='$sampleid' group by journal_number")) {
            echo "exec of sqlgetduplicate s failed" . $jndup->error;
        }
        
        $testdata = $sqlgetduplicate->fetch_array(MYSQLI_ASSOC);
        //no duplicates found
        $testsnpid = $testdata['SNPID'];
        //only want to upload 22 snp else update quality of pyrosequencing
        if($jndup->num_rows === 0){
            // check for multiple entrys @ genotyping table
            if ($sqlgetduplicate->num_rows === 0) {
            if (! ($sql->execute())) {
                echo "exec failed" . $sql->error;
            }
            if($numnum){
                if (! ($insertintosamplepyro = $db->query("insert into sample_pyro (journal_number,SNPID,quality)
                       values ('$sampleid','$testsnpid','$quality') "))) {
                       echo 'insert samplepyro error <br>' . $db->error;
                }
            }			
        } else {
            if($numnum){
                if (! ($insertintosamplepyro = $db->query("insert into sample_pyro (journal_number,SNPID,quality)
                    values ('$sampleid','$testsnpid','$quality') "))) {
                    echo 'insert samplepyro error <br>' . $db->error;
                }
            }           
        }
        }else{
            if (! ($updateintosamplepyro = $db->query("update sample_pyro set quality='$quality'where journal_number='$sampleid' and SNPID='$testsnpid')
                       "))) {
                       echo 'update samplepyro error <br>' . $db->error;
            }
        }
    } // end if(dom_import .....)
} // end for each
$sql->close();
echo "done";
//close sql connection 
$htmlStr = ob_get_contents();
//should delete
ob_end_clean(); 
     file_put_contents("XXX.txt", $htmlStr);
}
?>      