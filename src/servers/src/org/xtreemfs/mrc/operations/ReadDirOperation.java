/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

 This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
 Grid Operating System, see <http://www.xtreemos.eu> for more details.
 The XtreemOS project has been developed with the financial support of the
 European Commission's IST program under contract #FP6-033576.

 XtreemFS is free software: you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 2 of the License, or (at your option)
 any later version.

 XtreemFS is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.mrc.operations;

import java.util.Iterator;

import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_listdirRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_listdirResponse;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;

/**
 * 
 * @author stender
 */
public class ReadDirOperation extends MRCOperation {
    
    public ReadDirOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final xtreemfs_listdirRequest rqArgs = (xtreemfs_listdirRequest) rq.getRequestArgs();
        
        final VolumeManager vMan = master.getVolumeManager();
        final FileAccessManager faMan = master.getFileAccessManager();
        
        validateContext(rq);
        
        Path p = new Path(rqArgs.getPath());
        
        StorageManager sMan = vMan.getStorageManagerByName(p.getComp(0));
        PathResolver res = new PathResolver(sMan, p);
        
        // check whether the path prefix is searchable
        faMan.checkSearchPermission(sMan, res, rq.getDetails().userId, rq.getDetails().superUser, rq
                .getDetails().groupIds);
        
        // check whether file exists
        res.checkIfFileDoesNotExist();
        
        FileMetadata file = res.getFile();
        
        // check whether the directory grants read access
        faMan.checkPermission(FileAccessManager.O_RDONLY, sMan, file, res.getParentDirId(),
            rq.getDetails().userId, rq.getDetails().superUser, rq.getDetails().groupIds);
        
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
        
        // if required, update POSIX timestamps
        if (!master.getConfig().isNoAtime())
            MRCHelper.updateFileTimes(res.getParentDirId(), file, true, false, false, sMan, update);
        
        StringSet names = new StringSet();
        
        Iterator<FileMetadata> it = sMan.getChildren(res.getFile().getId());
        while (it.hasNext()) {
            
            String next = it.next().getFileName();
            
//            // ignore the .fuse-hidden directory
//            if (res.getFile().getId() == 1 && next.equals(".fuse-hidden"))
//                continue;
            
            names.add(next);
        }
        
        // set the response
        rq.setResponse(new xtreemfs_listdirResponse(names));
        
        update.execute();
        
    }
}
