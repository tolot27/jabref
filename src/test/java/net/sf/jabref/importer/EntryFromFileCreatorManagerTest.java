/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.importer;

import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @version 11.11.2008 | 21:51:54
 */
public class EntryFromFileCreatorManagerTest {

    @Test
    public void testGetCreator() throws Exception {
        EntryFromFileCreatorManager manager = new EntryFromFileCreatorManager();
        EntryFromFileCreator creator = manager.getEntryCreator(ImportDataTest.NOT_EXISTING_PDF);
        Assert.assertNull(creator);

        creator = manager.getEntryCreator(ImportDataTest.FILE_IN_DATABASE);
        Assert.assertNotNull(creator);
        Assert.assertTrue(creator.accept(ImportDataTest.FILE_IN_DATABASE));
    }

    @Test
    @Ignore
    public void testAddEntrysFromFiles() throws Exception {
        try (FileReader fr = new FileReader(ImportDataTest.UNLINKED_FILES_TEST_BIB)) {
            ParserResult result = BibtexParser.parse(fr);
            BibtexDatabase database = result.getDatabase();

            List<File> files = new ArrayList<>();

            files.add(ImportDataTest.FILE_NOT_IN_DATABASE);
            files.add(ImportDataTest.NOT_EXISTING_PDF);

            EntryFromFileCreatorManager manager = new EntryFromFileCreatorManager();
            List<String> errors = manager.addEntrysFromFiles(files, database, null, true);

            /**
             * One file doesn't exist, so adding it as an entry should lead to an error message.
             */
            Assert.assertEquals(1, errors.size());

            boolean file1Found = false;
            boolean file2Found = false;
            for (BibtexEntry entry : database.getEntries()) {
                String filesInfo = entry.getField("file");
                if (filesInfo.contains(files.get(0).getName())) {
                    file1Found = true;
                }
                if (filesInfo.contains(files.get(1).getName())) {
                    file2Found = true;
                }
            }

            Assert.assertTrue(file1Found);
            Assert.assertFalse(file2Found);
        }
    }

}
