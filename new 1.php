<?php    
/*
 * PHP QR Code encoder
 *
 * Exemplatory usage
 *
 * PHP QR Code is distributed under LGPL 3
 * Copyright (C) 2010 Dominik Dzienia <deltalab at poczta dot fm>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */
    
    echo "<h1>Генерация QR кодов</h1><hr/>";
    
    //set it to writable location, a place for temp generated PNG files
    $PNG_TEMP_DIR = dirname(__FILE__).DIRECTORY_SEPARATOR.'temp'.DIRECTORY_SEPARATOR;
    
    //html PNG location prefix
    $PNG_WEB_DIR = './temp/';

    include "qrlib.php";    
    $errorCorrectionLevel = 'L';
    $matrixPointSize = 10;
    
    //ofcourse we need rights to create temp dir
    if (!file_exists($PNG_TEMP_DIR))
        mkdir($PNG_TEMP_DIR);
if(isset($_FILES['userfile']['tmp_name']))
 {
 if (is_uploaded_file($_FILES['userfile']['tmp_name']))
  {
$file = file_get_contents($_FILES['userfile']['tmp_name'], true);
$ocad_wrk_dir=$PNG_TEMP_DIR.'ocad'.basename($_FILES['userfile']['tmp_name']);
mkdir($ocad_wrk_dir);
  }
 }
else
{
    $filename = $PNG_TEMP_DIR.'test.png';
//    $errorCorrectionLevel = 'L';
    if (isset($_REQUEST['level']) && in_array($_REQUEST['level'], array('L','M','Q','H')))
        $errorCorrectionLevel = 'L';    
//    $matrixPointSize = 10;
	$dat='STR';
	$cmd[0]=$dat;
    if (isset($_REQUEST['data'])) { 
        //it's very important!
        if (trim($_REQUEST['data']) == '')
            die('data cannot be empty! <a href="?">back</a>');
        // user data
        $filename = $PNG_TEMP_DIR.'test'.md5($_REQUEST['data'].'|'.$errorCorrectionLevel.'|'.$matrixPointSize).'.png';
		$dat=$_REQUEST['data'];
       QRcode::png($dat, $filename, $errorCorrectionLevel, $matrixPointSize, 2);   
	$cmd=explode(',',$dat);
	if($cmd[0]=='CRS' && count($cmd)>5)
		{
		@mkdir($PNG_TEMP_DIR.'crs'.md5($_REQUEST['data'].'|'.$errorCorrectionLevel.'|'.$matrixPointSize));
		$crs=$PNG_TEMP_DIR.'crs'.md5($_REQUEST['data'].'|'.$errorCorrectionLevel.'|'.$matrixPointSize).DIRECTORY_SEPARATOR.'crs.png';
		QRcode::png($dat, $crs, $errorCorrectionLevel, $matrixPointSize, 2);   
		for($i=2;$i<count($cmd)-3;$i++)
			{
			$crs=$PNG_TEMP_DIR.'crs'.md5($_REQUEST['data'].'|'.$errorCorrectionLevel.'|'.$matrixPointSize).DIRECTORY_SEPARATOR.$cmd[$i].'.png';
		    QRcode::png($cmd[$i], $crs, $errorCorrectionLevel, $matrixPointSize, 2);   
			$im = @imagecreatefrompng(dirname(__FILE__).DIRECTORY_SEPARATOR.'shablon'.DIRECTORY_SEPARATOR.'cp_shablon.png');
			$qr = @imagecreatefrompng($crs);
			imagecopy($im,$qr,25,25,0,0,250,250);
			$black = imagecolorallocate($im, 0, 0, 0);
			$font='./arial.ttf';
			imagettftext($im, 20, 0, 120, 520, $black, $font, $cmd[$i]);
			imagepng($im,$crs);
			imagedestroy($qr);
			imagedestroy($im);
			}
		copy(dirname(__FILE__).DIRECTORY_SEPARATOR.'shablon'.DIRECTORY_SEPARATOR.'str.png',$PNG_TEMP_DIR.'crs'.md5($_REQUEST['data'].'|'.$errorCorrectionLevel.'|'.$matrixPointSize).DIRECTORY_SEPARATOR.'str.png');
		copy(dirname(__FILE__).DIRECTORY_SEPARATOR.'shablon'.DIRECTORY_SEPARATOR.'fin.png',$PNG_TEMP_DIR.'crs'.md5($_REQUEST['data'].'|'.$errorCorrectionLevel.'|'.$matrixPointSize).DIRECTORY_SEPARATOR.'fin.png');
		copy(dirname(__FILE__).DIRECTORY_SEPARATOR.'shablon'.DIRECTORY_SEPARATOR.'DWN_QR.png',$PNG_TEMP_DIR.'crs'.md5($_REQUEST['data'].'|'.$errorCorrectionLevel.'|'.$matrixPointSize).DIRECTORY_SEPARATOR.'DWN_QR.png');
		copy(dirname(__FILE__).DIRECTORY_SEPARATOR.'shablon'.DIRECTORY_SEPARATOR.'DWN_SMS.png',$PNG_TEMP_DIR.'crs'.md5($_REQUEST['data'].'|'.$errorCorrectionLevel.'|'.$matrixPointSize).DIRECTORY_SEPARATOR.'DWN_SMS.png');
		@unlink($PNG_TEMP_DIR.'crs'.md5($_REQUEST['data'].'|'.$errorCorrectionLevel.'|'.$matrixPointSize).DIRECTORY_SEPARATOR.'course.zip');
//		exec('7z a '.$PNG_TEMP_DIR.'crs'.md5($_REQUEST['data'].'|'.$errorCorrectionLevel.'|'.$matrixPointSize).DIRECTORY_SEPARATOR.'course.zip '.$PNG_TEMP_DIR.'crs'.md5($_REQUEST['data'].'|'.$errorCorrectionLevel.'|'.$matrixPointSize).DIRECTORY_SEPARATOR.'*',$res);

   }
   else {    
    
        //default data
        
//		echo 'You can provide data in GET parameter: <a href="?data=like_that">like that</a><hr/>';   
       QRcode::png($dat, $filename, $errorCorrectionLevel, $matrixPointSize, 2);    
        
    }  
}
    echo '<form action="index.php" method="post">
	Генерирует один QR-код или все коды необходимые для дистанции <br>
        Данные:&nbsp;<input name="data" style="width:500px" value="'.(isset($_REQUEST['data'])?htmlspecialchars($_REQUEST['data']):'STR').'" />&nbsp;
        <input type="submit" value="Сгенерировать"></form><hr/>';
	echo '<form  enctype="multipart/form-data" action="index.php" method="post">
	<input type="hidden" name="MAX_FILE_SIZE" value="600000000">


Генерирует все коды необходимые для всех дистанций из файла OCAD Course Settings->Export->Courses Text<br>
к названию дистанции будет добавлено имя файл без .txt <br>
		Файл экспорта из OCAD:&nbsp;<input name="userfile" type="file" style="width:380px"  />&nbsp;
        <input type="submit" value="Сгенерировать"></form><hr/>';

if(!isset($_FILES['userfile']['tmp_name']))
	{
	  $im = @imagecreatefrompng(dirname(__FILE__).DIRECTORY_SEPARATOR.'shablon'.DIRECTORY_SEPARATOR.'cp_shablon.png');
	  $qr = @imagecreatefrompng($PNG_WEB_DIR.basename($filename));
	  if(imagesx ($qr)==250)
		{
//	    $qr = @imagecreatefrompng($PNG_WEB_DIR.basename($filename));
	    imagecopy($im,$qr,25,25,0,0,250,250);
		$black = imagecolorallocate($im, 0, 0, 0);
		$font='./arial.ttf';
		imagettftext($im, 20, 0, 120, 520, $black, $font, $dat);
		imagepng($im,$PNG_WEB_DIR.basename($filename));

		}
		imagedestroy($qr);
		imagedestroy($im);
	  echo '<img src="'.$PNG_WEB_DIR.basename($filename).'" />';     
	  if($cmd[0]=='CRS')
		{
		  echo("<A href='".$PNG_WEB_DIR.'crs'.md5($_REQUEST['data'].'|'.$errorCorrectionLevel.'|'.$matrixPointSize)."/course.zip"."'>Скачать все QR-коды этой дистанции</A>");
		}
	echo('<hr>');
	}
 else
	{
	if(is_uploaded_file($_FILES['userfile']['tmp_name']))
	  {
//	 echo $file.'<br>';
	 $fname=pathinfo($_FILES['userfile']['name'])['filename'];
	 $course=explode(PHP_EOL,$file);
	 $totalcp=array();
	 for($i=0;$i<count( $course);$i++)
		{
		$cpart= preg_split('/\s+/', $course[$i]);
	//		explode(' ',$course[$i]);
		$crs='CRS,STR,';
	//	print_r($cpart);
	//	echo count($cpart);
		if(count($cpart)<6 and count($cpart)>3)
		 {
		$cp=explode('-',$cpart[count($cpart)-1]);

		for($j=1;$j<count($cp)-2;$j++)
			{
			$crs=$crs.$cp[$j].',';
			for($k=0;$k<count($totalcp);$k++)
				{
				if($cp[$j]==$totalcp[$k])
					break;
				}
			$totalcp[$k]=$cp[$j];
			}
		  $l=str_replace(',','.',$cpart[1]);
		  $crs=$crs.'FIN,'.$fname.' '.$cpart[0].','.($l*1000);
		  $cname= iconv("UTF-8","CP1251",$cpart[0]);
		  $rv= iconv("UTF-8","CP1251","/[^a-zа-яA-ZА-Я0-9\_\-\.]/i");
		  $cname= preg_replace($rv, '',$cname);
		  $crsf=$ocad_wrk_dir.DIRECTORY_SEPARATOR.'crs'.$cname.'.png';
//				QRcode::png($crs, $crsf, $errorCorrectionLevel, $matrixPointSize, 2);  
				QRcode::png($crs, $crsf, 'L', 10, 2);  
		  echo('Обработана дистанция:'.$fname.' '.$cpart[0].'<BR>');
		 }
		 for($k=0;$k<count($totalcp);$k++)
				{
				if($cp[0]==$totalcp[$k])
					break;
				}
				$totalcp[$k]=$cp[0];
			
		 for($k=0;$k<count($totalcp);$k++)
				{
				if($cp[count($cp)-1]==$totalcp[$k])
					break;
				}
		$totalcp[$k]=$cp[count($cp)-1];
		
		}
	  }
	for($k=0;$k<count($totalcp);$k++)
		{
		$crsf=$ocad_wrk_dir.DIRECTORY_SEPARATOR.$totalcp[$k].'.png';
		if(substr($totalcp[$k],0,1)=='S')
			$totalcp[$k]='STR';
		if(substr($totalcp[$k],0,1)=='F')
			$totalcp[$k]='FIN';

//				QRcode::png($crs, $crsf, $errorCorrectionLevel, $matrixPointSize, 2);  
				QRcode::png($totalcp[$k], $crsf, 'L', 10, 2);  			
				$im = @imagecreatefrompng(dirname(__FILE__).DIRECTORY_SEPARATOR.'shablon'.DIRECTORY_SEPARATOR.'cp_shablon.png');
			$qr = @imagecreatefrompng($crsf);
			imagecopy($im,$qr,25,25,0,0,250,250);
			$black = imagecolorallocate($im, 0, 0, 0);
			$font='./arial.ttf';
			imagettftext($im, 20, 0, 120, 520, $black, $font, $totalcp[$k]);
			imagepng($im,$crsf);
			imagedestroy($qr);
			imagedestroy($im);

		}
		copy(dirname(__FILE__).DIRECTORY_SEPARATOR.'shablon'.DIRECTORY_SEPARATOR.'DWN_QR.png',$ocad_wrk_dir.DIRECTORY_SEPARATOR.'DWN_QR.png');
		copy(dirname(__FILE__).DIRECTORY_SEPARATOR.'shablon'.DIRECTORY_SEPARATOR.'DWN_SMS.png',$ocad_wrk_dir.DIRECTORY_SEPARATOR.'DWN_SMS.png');
		@unlink($ocad_wrk_dir.DIRECTORY_SEPARATOR.'course.zip');
		
		//exec('7z a '.$ocad_wrk_dir.DIRECTORY_SEPARATOR.'course.zip '.$ocad_wrk_dir.DIRECTORY_SEPARATOR.'*',$res);
		$zip = new ZipArchive;
if ($zip->open($ocad_wrk_dir.DIRECTORY_SEPARATOR.'course.zip', ZipArchive::OVERWRITE) === TRUE)
{
    if ($handle = opendir($ocad_wrk_dir))
    {
        // Add all files inside the directory
        while (false !== ($entry = readdir($handle)))
        {
            if ($entry != "." && $entry != ".." && !is_dir($ocad_wrk_dir.DIRECTORY_SEPARATOR.$entry))
            {
                $zip->addFile($ocad_wrk_dir.DIRECTORY_SEPARATOR. $entry);
            }
        }
        closedir($handle);
    }
 
    $zip->close();
}
	echo("<A href='".$PNG_WEB_DIR."ocad".basename($_FILES['userfile']['tmp_name'])."/course.zip"."'>Все QR-коды файла</A>");
	}
    // benchmark
   // QRtools::timeBenchmark();    


   