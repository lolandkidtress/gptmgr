

// nohup java -jar /home/java/mgr/TCG.jar >> nohup.log 2>&1 &

CREATE TABLE CodeUser
( id VARCHAR(32) NOT NULL PRIMARY KEY,
 deleteflg BIGINT(1) NOT NULL DEFAULT '0' ,
 createtime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ,
 updatetime TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ,
 openid VARCHAR(32) ,
 unionid VARCHAR(32),
 appid VARCHAR(32),
 apikey VARCHAR(32)
) ENGINE = InnoDB;

alter table CodeUser add unique key pk_CodeUser (openid);

CREATE TABLE CodeUserQuota
( id VARCHAR(32) NOT NULL PRIMARY KEY,
 deleteflg BIGINT(1) NOT NULL DEFAULT '0' ,
 updatetime TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ,
 openid VARCHAR(32) ,
 cnt int,
 maxcnt int
) ENGINE = InnoDB;
alter table CodeUserQuota add unique key pk_CodeUserQuota (openid);


alter table CodeUser add invitecode VARCHAR(32);

CREATE TABLE InviteCode
( id VARCHAR(32) NOT NULL PRIMARY KEY,
 deleteflg BIGINT(1) NOT NULL DEFAULT '0' ,
 createtime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ,
 updatetime TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ,
 code VARCHAR(32) ,
 produceopenid VARCHAR(32),
 consumeopenid VARCHAR(32)
) ENGINE = InnoDB;
alter table InviteCode add unique key pk_InviteCode (produceopenid,code,consumeopenid);


CREATE TABLE ChatHist
( id VARCHAR(32) NOT NULL PRIMARY KEY,
 deleteflg BIGINT(1) NOT NULL DEFAULT '0' ,
 createtime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ,
 updatetime TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ,
 openid VARCHAR(32) ,
 question VARCHAR(32),
 result text
) ENGINE = InnoDB;