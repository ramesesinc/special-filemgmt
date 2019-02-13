CREATE DATABASE filemgmt;
USE filemgmt;

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for sys_file
-- ----------------------------
DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file` (
  `objid` varchar(50) NOT NULL,
  `title` varchar(50) DEFAULT NULL,
  `filetype` varchar(50) DEFAULT NULL,
  `dtcreated` datetime DEFAULT NULL,
  `createdby_objid` varchar(50) DEFAULT NULL,
  `createdby_name` varchar(255) DEFAULT NULL,
  `keywords` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`objid`),
  KEY `ix_dtcreated` (`dtcreated`),
  KEY `ix_createdby_objid` (`createdby_objid`),
  KEY `ix_keywords` (`keywords`),
  KEY `ix_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for sys_fileitem
-- ----------------------------
DROP TABLE IF EXISTS `sys_fileitem`;
CREATE TABLE `sys_fileitem` (
  `objid` varchar(50) NOT NULL,
  `state` varchar(50) DEFAULT NULL,
  `parentid` varchar(50) DEFAULT NULL,
  `dtcreated` datetime DEFAULT NULL,
  `createdby_objid` varchar(50) DEFAULT NULL,
  `createdby_name` varchar(255) DEFAULT NULL,
  `caption` varchar(155) DEFAULT NULL,
  `remarks` varchar(255) DEFAULT NULL,
  `filelocid` varchar(50) DEFAULT NULL,
  `filesize` bigint(20) DEFAULT NULL,
  `bytestransferred` bigint(20) NOT NULL,
  `thumbnail` text,
  PRIMARY KEY (`objid`),
  KEY `doc_album_entry_parent` (`parentid`),
  KEY `doc_album_entry_fileloc` (`filelocid`),
  CONSTRAINT `doc_album_entry_parent` FOREIGN KEY (`parentid`) REFERENCES `sys_file` (`objid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for sys_fileloc
-- ----------------------------
DROP TABLE IF EXISTS `sys_fileloc`;
CREATE TABLE `sys_fileloc` (
  `objid` varchar(50) NOT NULL,
  `url` varchar(255) NOT NULL,
  `filepath` varchar(255) DEFAULT NULL,
  `defaultloc` int(11) NOT NULL,
  `loctype` varchar(20) DEFAULT NULL,
  `user_name` varchar(50) DEFAULT NULL,
  `user_pwd` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`objid`),
  KEY `ix_loctype` (`loctype`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


INSERT INTO `sys_fileloc` (`objid`, `url`, `filepath`, `defaultloc`, `loctype`, `user_name`, `user_pwd`) 
VALUES ('default', '127.0.0.1:2121', NULL, '1', 'ftp', 'admin', 'admin');
