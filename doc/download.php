<html>
<head>
<title>Java Channel Access</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link rel="stylesheet" href="jca.css" type="text/css">
</head>
<script language="JavaScript">
function query(url) {
    if (confirm("Do ya really wanna go?")) location.href = url
}
</script>
<body bgcolor="#FFFFFF" text="#000000">
<table width="100%" height="100%" border="0" cellspacing="0" cellpadding="0">
  <tr> 
    <td height="1"><img height="1" width="200" src="spacer.gif"></td>
    <td><img height="1" width="10" src="spacer.gif"></td>
    <td width="100%"><img height="1" width="400" src="spacer.gif"></td>
    <td><img height="1" width="216" src="spacer.gif"></td>
  </tr>
  <tr> 
    <td height="110" colspan="2" align="center"><img src="duke_plugin.gif" width="85" height="99"></td>
    <td class="header" > <span class="header">JCA 2.1.2</span> </td>
    <td valign="top" align="right"><img src="apslogo.gif"></td>
  </tr>
  <tr> 
    <td height="30" colspan="4"></td>
  </tr>
  <tr> 
    <td width="200" align="left" valign="top"> 
      <table width="100%" height="100%" border="1" cellspacing="0" cellpadding="0" bordercolor="#48669F">
        <tr> 
          <td class="title2" width="706"><span class="title2"><a href="../index.html" class="title2">JCA</a></span> 
          </td>
        </tr>
        <tr> 
          <td bordercolor="#FFFFFF"><a href="http://www.aps.anl.gov/epics/" class="tab">- 
            EPICS Home Page</a></td>
        </tr>
        <tr> 
          <td height="3" bordercolor="#FFFFFF" background="line-light.png"></td>
        </tr>
        <tr> 
          <td bordercolor="#FFFFFF"><a href="http://www.aps.anl.gov/epics/modules/base/R3-14/4-docs/CAref.html" class="tab">- 
            Channel Access(3.14)</a></td>
        </tr>
        <tr> 
          <td height="3" bordercolor="#FFFFFF" background="line-light.png"></td>
        </tr>
        <tr> 
          <td height="30" bordercolor="#FFFFFF"></td>
        </tr>
        <tr> 
          <td class="title2"><span class="title2"><a href="index.html" class="title2">JCA2.1.2</a></span> 
          </td>
        </tr>
        <tr> 
          <td bordercolor="#FFFFFF"><a href="download.html" class="tab">- Download</a></td>
        </tr>
        <tr> 
          <td height="3" bordercolor="#FFFFFF" background="line-light.png"></td>
        </tr>
        <tr> 
          <td bordercolor="#FFFFFF"><a href="installation.html" class="tab">- Installation</a></td>
        </tr>
        <tr> 
          <td height="3" bordercolor="#FFFFFF" background="line-light.png"></td>
        </tr>
        <tr> 
          <td bordercolor="#FFFFFF"><a href="api/index.html" class="tab">- API 
            documentation </a></td>
        </tr>
        <tr> 
          <td height="3" bordercolor="#FFFFFF" background="line-light.png"></td>
        </tr>
        <tr> 
          <td bordercolor="#FFFFFF"><a href="tutorial.html" class="tab">- Tutorial </a></td>
        </tr>
        <tr> 
          <td height="3" bordercolor="#FFFFFF" background="line-light.png"></td>
        </tr>
        <tr> 
          <td height="30" bordercolor="#FFFFFF"></td>
        </tr>
        <tr> 
          <td height="100%" bordercolor="#FFFFFF"></td>
        </tr>
      </table>
    </td>
    <td width="10"></td>
    <td colspan="2" align="left" valign="top"  > 
      <table width="100%" height="100%" border="0" cellspacing="0" cellpadding="0">
        <tr> 
          <td class="title1"><span class="title1">Download</span></td>
        </tr>
        <tr> 
          <td> 
            <table width="100%" border="0" cellspacing="0" cellpadding="0">
              <tr> 
                <td height="1" width="250"><img src="spacer.gif" height="1" width="250"></td>
                <td height="1"></td>
              </tr>
              <tr valign="bottom"> 
                <td colspan="2" height="30"><b>Sources distributions</b></td>
              </tr>
              <tr> 
                <td colspan="2" height="1" background="line-dark.png"></td>
              </tr>
			  <tr>
			  <td width="30%">
				<?php 
				
function remote_file_exists ($url) 
{ 

/*
   Return error codes:
   1 = Invalid URL host
   2 = Unable to connect to remote host
*/    

   $head = ""; 
   $url_p = parse_url ($url); 
   
   if (isset ($url_p["host"])) 
   { $host = $url_p["host"]; } 
   else 
   { return 1; } 
   
   if (isset ($url_p["path"])) 
   { $path = $url_p["path"]; } 
   else 
   { $path = ""; } 
   
   $fp = fsockopen ($host, 80, $errno, $errstr, 20); 
   if (!$fp) 
   { return 2; } 
   else 
   { 
       $parse = parse_url($url); 
       $host = $parse['host']; 
       
       fputs($fp, "HEAD ".$url." HTTP/1.1\r\n"); 
       fputs($fp, "HOST: ".$host."\r\n"); 
       fputs($fp, "Connection: close\r\n\r\n"); 
       $headers = ""; 
       while (!feof ($fp)) 
       { $headers .= fgets ($fp, 128); } 
   } 
   fclose ($fp); 
   $arr_headers = explode("\n", $headers); 
   $return = false; 
   if (isset ($arr_headers[0])) 
   { $return = strpos ($arr_headers[0], "404") === false; } 
   return $return; 
} 
				
				
if (remote_file_exists('http://www.aps.anl.gov/xfd/SoftDist/swBCDA/jca/2.1/download/jca2.1-src.tgz')) {
   print "Le fichier existe";
} else {
   print "Le fichier n'existe pas";
}
?>
</td><td>coucou</td>
</tr>
              <tr> 
               <td width="30%">
		
				<a href="http://www.aps.anl.gov/xfd/SoftDist/swBCDA/jca/download/jca2.1.2-src.tgz" class="tab2">- 
                  jca2.1.2-src.tgz</a></td>
                <td>Source files in tgz format</td>
              </tr>
			  <tr>
               <td width="30%">
		 		<a href="http://www.aps.anl.gov/xfd/SoftDist/swBCDA/jca/2.1/download/jca2.1-src.tgz" class="tab2">- 
                  jca2.1-src.tgz</a></td>
                <td>Source files in tgz format</td>
              </tr>
              <tr> 
                <td colspan="2" height="1" bordercolor="#FFFFFF" background="line-dark.png"></td>
              </tr>
              <tr> 
                <td><a href="http://www.aps.anl.gov/xfd/SoftDist/swBCDA/jca/download/jca2.1.2-src.zip" class="tab2">- 
                  jca2.1.2-src.zip</a></td>
                <td>Source files in zip format</td>
              </tr>
              <tr> 
                <td colspan="2" height="1" bordercolor="#FFFFFF" background="line-dark.png"></td>
              </tr>
              <tr valign="bottom"> 
                <td colspan="2" height="30"><b>Binary distributions</b></td>
              </tr>
              <tr> 
                <td colspan="2" height="1" bordercolor="#FFFFFF" background="line-dark.png"></td>
              </tr>
              <tr> 
                <td>- jca2.1.2-solaris-sparc.tgz</td>
                <td>Binaries for Solaris-sparc platforms (<font color="#FF0000">Not 
                  yet available</font>)</td>
              </tr>
              <tr> 
                <td colspan="2" height="1" bordercolor="#FFFFFF" background="line-dark.png"></td>
              </tr>
              <tr> 
                <td><a href="http://www.aps.anl.gov/xfd/SoftDist/swBCDA/jca/download/jca2.1.2-linux-x86.tgz" class="tab2">- 
                  jca2.1.2-linux-x86.tgz</a></td>
                <td>Binaries for Linux-x86 platforms</td>
              </tr>
              <tr> 
                <td colspan="2" height="1" bordercolor="#FFFFFF" background="line-dark.png"></td>
              </tr>
              <tr> 
                <td>- jca2.1.2-darwin-ppc.tgz</td>
                <td>Binaries for Mac OS X-darwin-ppc platforms (<font color="#FF0000">Not 
                  yet available</font>)</td>
              </tr>
              <tr> 
                <td colspan="2" height="1" bordercolor="#FFFFFF" background="line-dark.png"></td>
              </tr>
              <tr> 
                <td><a href="http://www.aps.anl.gov/xfd/SoftDist/swBCDA/jca/download/jca2.1.2-win32-x86.zip" class="tab2">- 
                  jca2.1.2-win32-x86.zip</a></td>
                <td>Binaries for windows platforms</td>
              </tr>
              <tr> 
                <td colspan="2" height="1" bordercolor="#FFFFFF" background="line-dark.png"></td>
              </tr>
              <tr valign="bottom"> 
                <td colspan="2" height="30"><b>Documentation</b></td>
              </tr>
              <tr> 
                <td colspan="2" height="1" bordercolor="#FFFFFF" background="line-dark.png"></td>
              </tr>
              <tr> 
                <td><a href="http://www.aps.anl.gov/xfd/SoftDist/swBCDA/jca/download/jca2.1.2-doc.tgz" class="tab2">- 
                  jca2.1.2-doc.tgz</a></td>
                <td>documentation in tgz format</td>
              </tr>
              <tr> 
                <td colspan="2" height="1" bordercolor="#FFFFFF" background="line-dark.png"></td>
              </tr>
              <tr> 
                <td><a href="http://www.aps.anl.gov/xfd/SoftDist/swBCDA/jca/download/jca2.1.2-doc.zip" class="tab2">- 
                  jca2.1.2-doc.zip</a></td>
                <td>documentation in zip format</td>
              </tr>
              <tr> 
                <td colspan="2" height="1" bordercolor="#FFFFFF" background="line-dark.png"></td>
              </tr>
            </table>
            <p>&nbsp;</p>
            <p>&nbsp;</p>
            <p>&nbsp;</p>
          </td>
        </tr>
        <tr> 
          <td height="100%"> </td>
        </tr>
      </table>
    </td>
  </tr>
</table>
</body>
</html>
