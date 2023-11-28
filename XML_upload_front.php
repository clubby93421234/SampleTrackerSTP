<?php
#	ini_set('session.cache_limiter','public');
#	session_cache_limiter(false);
#	header('Cache-Control: max-age=900');
	session_start();
	if(isset($_SESSION["DB"])){
		$user = $_SESSION["username"];
		$pwd = $_SESSION["pwd"];
		$db = @mysqli_connect("10.10.89.80:3306", "$user", "$pwd","inhousedatabase");
	}else{
		header ("Location: Patindex.php");
	}

?>

<html>
 <head>
	<title>In House Database Humangenetik Mainz</title>    
    <link rel="stylesheet" media="screen" type="text/css" href="nettes.css" title="Nett"/>
	<link rel="stylesheet" media="print" type="text/css" href="printerfriendly.css" />
	<link href="css/dropzone.css" type="text/css" rel="stylesheet" />
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
 </head>


<SCRIPT  TYPE="text/javascript" src="dropzone.js">

</SCRIPT>


 <body>
 
  
	<div id="ueberschriftHTML"> 
		<h1 style="color:#fdf4c2;">IN HOUSE DATABASE - HUMANGENETIK MAINZ</h1>
	</div>
	<div id="ueberschriftPRINT"> 
		<h1 style="color:#fdf4c2;">Aktennotiz</h1>
	</div>
	

    	<div class="left">
        	<?php include("menu.php") ?>
   		</div>
		
		<div class="middle">
			<?php if(isset($_SESSION['DB'])){ ?>
				<p style=text-align:right>
					<b><?php echo $_SESSION['username']; ?></b> ist angemeldet!
				</p>
			<?php } ?>
			<form action="xmlpyroreader.php" class="dropzone" id="myAwesomeDropzone" METHOD="POST" enctype="multipart/form-data">
 
</form>
			<!-- Ab hier kannst du alles mit Inhalt füllen
			Upload function is in form-node. see dropzonejs.com for more help with uploading files
			
			
			
			
			
			-->
			
		</div>
 </body>
</html>