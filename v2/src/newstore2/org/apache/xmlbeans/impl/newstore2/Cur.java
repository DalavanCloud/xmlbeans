/*   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.xmlbeans.impl.newstore2;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.DOMImplementation;

// DOM Level 3
import org.w3c.dom.UserDataHandler;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.TypeInfo;

import javax.xml.soap.Detail;
import javax.xml.soap.DetailEntry;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPPart;
import javax.xml.soap.SOAPFaultElement;

import javax.xml.transform.Source;

import java.io.PrintStream;

import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import org.apache.xmlbeans.impl.newstore2.Xobj.Bookmark;

import org.apache.xmlbeans.impl.newstore2.Locale.LoadContext;

import org.apache.xmlbeans.impl.newstore2.DomImpl.Dom;
import org.apache.xmlbeans.impl.newstore2.DomImpl.CharNode;
import org.apache.xmlbeans.impl.newstore2.DomImpl.TextNode;
import org.apache.xmlbeans.impl.newstore2.DomImpl.CdataNode;
import org.apache.xmlbeans.impl.newstore2.DomImpl.SaajTextNode;
import org.apache.xmlbeans.impl.newstore2.DomImpl.SaajCdataNode;

import org.apache.xmlbeans.XmlBeans;
import org.apache.xmlbeans.SchemaField;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.SchemaTypeLoader;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.QNameSet;

import org.apache.xmlbeans.impl.values.TypeStore;
import org.apache.xmlbeans.impl.values.TypeStoreUser;
import org.apache.xmlbeans.impl.values.TypeStoreVisitor;
import org.apache.xmlbeans.impl.values.TypeStoreUserFactory;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.impl.common.ValidatorListener;
import org.apache.xmlbeans.impl.common.XmlLocale;
import org.apache.xmlbeans.impl.common.QNameHelper;


final class Cur
{
    static final int TEXT     = 0; // Must be 0
    static final int ROOT     = 1;
    static final int ELEM     = 2;
    static final int ATTR     = 3;
    static final int COMMENT  = 4;
    static final int PROCINST = 5;

    static final int POOLED       = 0;
    static final int REGISTERED   = 1;
    static final int EMBEDDED     = 2;
    static final int DISPOSED     = 3;

    static final int END_POS = -1;
    static final int NO_POS  = -2;
    
    Cur ( Locale l )
    {
        _locale = l;
        _pos = NO_POS;
        
        _tempFrame = -1;
        
        _state = POOLED;

        _stackTop = Locations.NULL;
        _selectionFirst = -1;
        _selectionN = -1;
        _selectionLoc = Locations.NULL;
        _selectionCount = 0;
    }

    boolean isPositioned ( ) { assert isNormal(); return _xobj != null; }

    static boolean kindIsContainer ( int k ) { return k ==  ELEM || k ==  ROOT; }
    static boolean kindIsFinish    ( int k ) { return k == -ELEM || k == -ROOT; }
    static boolean kindIsUserNode  ( int k ) { return k ==  ELEM || k ==  ATTR || k == ROOT; }
    
    int kind ( )
    {
        assert isPositioned();
        return _pos == 0 ? _xobj.kind() : _pos == END_POS ? -_xobj.kind() : TEXT;
    }

    boolean isRoot      ( ) { assert isPositioned(); return _pos == 0 && _xobj.kind() == ROOT;     }
    boolean isElem      ( ) { assert isPositioned(); return _pos == 0 && _xobj.kind() == ELEM;     }
    boolean isAttr      ( ) { assert isPositioned(); return _pos == 0 && _xobj.kind() == ATTR;     }
    boolean isComment   ( ) { assert isPositioned(); return _pos == 0 && _xobj.kind() == COMMENT;  }
    boolean isProcinst  ( ) { assert isPositioned(); return _pos == 0 && _xobj.kind() == PROCINST; }
    boolean isText      ( ) { assert isPositioned(); return _pos > 0; }
    boolean isEnd       ( ) { assert isPositioned(); return _pos == END_POS && _xobj.kind() ==ELEM;}
    boolean isNode      ( ) { assert isPositioned(); return _pos == 0; }
    boolean isContainer ( ) { assert isPositioned(); return _pos == 0       && kindIsContainer( _xobj.kind() ); }
    boolean isFinish    ( ) { assert isPositioned(); return _pos == END_POS && kindIsContainer( _xobj.kind() ); }
    boolean isUserNode  ( ) { assert isPositioned(); return _pos == 0       && kindIsUserNode ( _xobj.kind() ); }
    
    boolean isNormalAttr ( ) { assert isNode(); return _xobj.isNormalAttr(); }
    boolean isXmlns      ( ) { assert isNode(); return _xobj.isXmlns(); }

    QName   getName  ( ) { assert isNode() || isEnd(); return _xobj._name; }
    String  getLocal ( ) { return getName().getLocalPart(); }
    String  getUri   ( ) { return getName().getNamespaceURI(); }

    String  getXmlnsPrefix ( ) { assert isXmlns(); return _xobj.getXmlnsPrefix(); }
    String  getXmlnsUri    ( ) { assert isXmlns(); return _xobj.getXmlnsUri(); }

    boolean isDomDocRoot  ( ) { return isRoot() && _xobj.getDom() instanceof Document; }
    boolean isDomFragRoot ( ) { return isRoot() && _xobj.getDom() instanceof DocumentFragment; }

    private int cchRight ( ) { assert isPositioned(); return _xobj.cchRight( _pos ); }
    private int cchLeft  ( ) { assert isPositioned(); return _xobj.cchLeft ( _pos ); }
    
    //
    // Creation methods
    //

    void createRoot ( )
    {
        createDomDocFragRoot();
    }
    
    void createDomDocFragRoot ( )
    {
        moveTo( new Xobj.DocumentFragXobj( _locale ) );
    }
    
    void createDomDocumentRoot ( )
    {
        moveTo( createDomDocumentRootXobj( _locale ) );
    }
    
    void createAttr ( QName name )
    {
        createHelper( new Xobj.AttrXobj( _locale, name ) );
    }
    
    void createComment ( )
    {
        createHelper( new Xobj.CommentXobj( _locale ) );
    }
    
    void createProcinst ( String target )
    {
        createHelper( new Xobj.ProcInstXobj( _locale, target ) );
    }
    
    void createElement ( QName name )
    {
        createElement( name, null );
    }
    
    void createElement ( QName name, QName parentName )
    {
        createHelper( createElementXobj( _locale, name, parentName ) );
    }

    static Xobj createDomDocumentRootXobj ( Locale l )
    {
        Xobj xo;

        if (l._saaj == null)
            xo = new Xobj.DocumentXobj( l );
        else
            xo = new Xobj.SoapPartDocXobj( l );
        
        if (l._ownerDoc == null)
            l._ownerDoc = xo.getDom();

        return xo;
    }

    static Xobj createElementXobj ( Locale l, QName name, QName parentName )
    {
        if (l._saaj == null)
            return new Xobj.ElementXobj( l, name );
        
        Class c = l._saaj.identifyElement( name, parentName );

        if (c == SOAPElement.class)       return new Xobj.SoapElementXobj       ( l, name );
        if (c == SOAPBody.class)          return new Xobj.SoapBodyXobj          ( l, name );
        if (c == SOAPBodyElement.class)   return new Xobj.SoapBodyElementXobj   ( l, name );
        if (c == SOAPEnvelope.class)      return new Xobj.SoapEnvelopeXobj      ( l, name );
        if (c == SOAPHeader.class)        return new Xobj.SoapHeaderXobj        ( l, name );
        if (c == SOAPHeaderElement.class) return new Xobj.SoapHeaderElementXobj ( l, name );
        if (c == SOAPFaultElement.class)  return new Xobj.SoapFaultElementXobj  ( l, name );
        if (c == Detail.class)            return new Xobj.DetailXobj            ( l, name );
        if (c == DetailEntry.class)       return new Xobj.DetailEntryXobj       ( l, name );
        if (c == SOAPFault.class)         return new Xobj.SoapFaultXobj         ( l, name );
        
        throw new IllegalStateException( "Unknown SAAJ element class: " + c );
    }
            
    private void createHelper ( Xobj x )
    {
        assert x._locale == _locale;

        // insert the new Xobj into an exisiting tree.
        
        if (isPositioned())
        {
            Cur from = tempCur( x, 0 );
            from.moveNode( this );
            from.release();
        }

        moveTo( x );
    }
    
    //
    // General operations
    //

    boolean isSamePos ( Cur that )
    {
        assert isNormal() && (that == null || that.isNormal());
        
        return _xobj == that._xobj && _pos == that._pos;
    }

    // is this just after the end of that (that must be the start of a node)
    
    boolean isJustAfterEnd ( Cur that )
    {
        assert isNormal() && that != null && that.isNormal() && that.isNode();

        return that._xobj.isJustAfterEnd( _xobj, _pos );
    }
    
    boolean isJustAfterEnd ( Xobj x )
    {
        return x.isJustAfterEnd( _xobj, _pos );
    }
    
    boolean isAtEndOf ( Cur that )
    {
        assert that != null && that.isNormal() && that.isNode();

        return _xobj == that._xobj && _pos == END_POS;
    }

    void setName ( QName newName )
    {
        assert isNode() && newName != null;

        _xobj.setName( newName );
    }
    
    void moveTo ( Xobj x )
    {
        moveTo( x, 0 );
    }
    
    void moveTo ( Xobj x, int p )
    {
        // This cursor may not be normalized upon entry, don't assert isNormal() here

        assert x == null || _locale == x._locale;
        assert x != null || p == NO_POS;
        assert x == null || x.isNormal( p );
        assert _state == REGISTERED || _state == EMBEDDED;
        assert _state == EMBEDDED || (_xobj == null || !isOnList( _xobj._embedded ));
        assert _state == REGISTERED || (_xobj != null && isOnList( _xobj._embedded ));

        moveToNoCheck( x, p );

        assert isNormal();
    }
    
    void moveToNoCheck ( Xobj x, int p )
    {
        if (_state == EMBEDDED && x != _xobj)
        {
            _xobj._embedded = listRemove( _xobj._embedded );
            _locale._registered = listInsert( _locale._registered );
            _state = REGISTERED;
        }
        
        _xobj = x;
        _pos = p;
    }

    void moveToCur ( Cur to )
    {
        assert isNormal() && (to == null || to.isNormal());
        
        if (to == null)
            moveTo( null, NO_POS );
        else
            moveTo( to._xobj, to._pos );
    }

    void moveToDom ( Dom d )
    {
        assert _locale == d.locale();
        assert d instanceof Xobj || d instanceof Xobj.SoapPartDom;

        moveTo( d instanceof Xobj ? (Xobj) d : ((Xobj.SoapPartDom) d)._docXobj );
    }

    static final class Locations
    {
        private static final int NULL = -1;
        
        Locations ( Locale l )
        {
            _locale = l;
            
            _xobjs = new Xobj [ _initialSize ];
            _poses = new int  [ _initialSize ];
            _curs  = new Cur  [ _initialSize ];
            _next  = new int  [ _initialSize ];
            _prev  = new int  [ _initialSize ];
            _nextN = new int  [ _initialSize ];
            _prevN = new int  [ _initialSize ];

            _next [ _initialSize - 1 ] = NULL;

            for ( int i = _initialSize - 2 ; i >= 0 ; i-- )
            {
                assert _xobjs[ i ] == null;
                _poses [ i ] = NO_POS;
                _next  [ i ] = i + 1;
                _prev  [ i ] = NULL;
                _nextN [ i ] = NULL;
                _prevN [ i ] = NULL;
            }

            _free = 0;
            _naked = NULL;
        }

        boolean isSamePos ( int i, Cur c )
        {
            if (_curs[ i ] == null)
                return c._xobj == _xobjs[ i ] && c._pos == _poses[ i ];
            else
                return c.isSamePos( _curs[ i ] );
        }
        
        boolean isAtEndOf ( int i, Cur c )
        {
            assert _poses[ i ] == 0;
            
            if (_curs[ i ] == null)
                return c._xobj == _xobjs[ i ] && c._pos == END_POS;
            else
                return c.isAtEndOf( _curs[ i ] );
        }

        void moveTo ( int i, Cur c )
        {
            if (_curs[ i ] == null)
                c.moveTo( _xobjs[ i ], _poses[ i ] );
            else
                c.moveToCur( _curs[ i ] );
        }

        int insert ( int head, int before, int i )
        {
            return insert( head, before, i, _next, _prev );
        }

        int remove ( int head, int i )
        {
            Cur c = _curs[ i ];
            
            assert c != null || _xobjs[ i ] != null;
            assert c != null || _xobjs[ i ] != null;

            if (c != null)
            {
                _curs[ i ].release();
                _curs[ i ] = null;

                assert _xobjs[ i ] == null;
                assert _poses [ i ] == NO_POS;
            }
            else
            {
                assert _xobjs[ i ] != null && _poses[ i ] != NO_POS;
                
                _xobjs[ i ] = null;
                _poses[ i ] = NO_POS;
                
                _naked = remove( _naked, i, _nextN, _prevN );
            }
            
            head = remove( head, i, _next, _prev );
            
            _next[ i ] = _free;
            _free = i;

            return head;
        }

        int allocate ( Cur addThis )
        {
            assert addThis.isPositioned();
            
            if (_free == NULL)
                makeRoom();

            int i = _free;
            
            _free = _next [ i ];

            _next [ i ] = NULL;
            assert _prev [ i ] == NULL;

            assert _curs [ i ] == null;
            assert _xobjs[ i ] == null;
            assert _poses[ i ] == NO_POS;

            _xobjs [ i ] = addThis._xobj;
            _poses [ i ] = addThis._pos;

            _naked = insert( _naked, NULL, i, _nextN, _prevN );

            return i;
        }

        private static int insert ( int head, int before, int i, int[] next, int[] prev )
        {
            if (head == NULL)
            {
                assert before == NULL;
                prev[ i ] = i;
                head = i;
            }
            else if (before != NULL)
            {
                prev[ i ] = prev[ before ];
                next[ i ] = before;
                prev[ before ] = i;

                if (head == before)
                    head = i;
            }
            else
            {
                prev[ i ] = prev[ head ];
                assert next[ i ] == NULL;
                next[ prev[ head ] ] = i;
                prev[ head ] = i;
            }

            return head;
        }

        private static int remove ( int head, int i, int[] next, int[] prev )
        {
            if (prev[ i ] == i)
            {
                assert head == i;
                head = NULL;
            }
            else
            {
                if (head == i)
                    head = next[ i ];
                else
                    next[ prev [ i ] ] = next[ i ];

                if (next[ i ] == NULL)
                    prev[ head ] = prev[ i ];
                else
                {
                    prev[ next[ i ] ] = prev[ i ];
                    next[ i ] = NULL;
                }
            }

            prev[ i ] = NULL;
            assert next[ i ] == NULL;

            return head;
        }

        void notifyChange ( )
        {
            for ( int i ; (i = _naked) != NULL ; )
            {
                assert _curs[ i ] == null && _xobjs[ i ] != null && _poses[ i ] != NO_POS;
                
                _naked = remove( _naked, i, _nextN, _prevN );

                _curs[ i ] = _locale.getCur();
                _curs[ i ].moveTo( _xobjs[ i ], _poses[ i ] );
                
                _xobjs[ i ] = null;
                _poses[ i ] = NO_POS;
            }
        }

        int next ( int i ) { return _next[ i ]; }
        int prev ( int i ) { return _prev[ i ]; }

        private void makeRoom ( )
        {
            assert _free == NULL;
            
            int l = _xobjs.length;

            Xobj [] oldXobjs = _xobjs;
            int  [] oldPoses = _poses;
            Cur  [] oldCurs  = _curs;
            int  [] oldNext  = _next;
            int  [] oldPrev  = _prev;
            int  [] oldNextN = _nextN;
            int  [] oldPrevN = _prevN;

            _xobjs = new Xobj [ l * 2 ];
            _poses = new int  [ l * 2 ];
            _curs  = new Cur  [ l * 2 ];
            _next  = new int  [ l * 2 ];
            _prev  = new int  [ l * 2 ];
            _nextN = new int  [ l * 2 ];
            _prevN = new int  [ l * 2 ];

            System.arraycopy( oldXobjs, 0, _xobjs, 0, l );
            System.arraycopy( oldPoses,  0, _poses, 0, l );
            System.arraycopy( oldCurs,  0, _curs,  0, l );
            System.arraycopy( oldNext,  0, _next,  0, l );
            System.arraycopy( oldPrev,  0, _prev,  0, l );
            System.arraycopy( oldNextN, 0, _nextN, 0, l );
            System.arraycopy( oldPrevN, 0, _prevN, 0, l );

            _next [ l * 2 - 1 ] = NULL;

            for ( int i = l * 2 - 2 ; i >= l ; i-- )
            {
                _next  [ i ] = i + 1;
                _prev  [ i ] = NULL;
                _nextN [ i ] = NULL;
                _prevN [ i ] = NULL;
                _poses [ i ] = NO_POS;
            }

            _free = l;
        }

        private static final int _initialSize = 32;

        private Locale _locale;
        
        private Xobj [] _xobjs;
        private int  [] _poses;
        private Cur  [] _curs;
        private int  [] _next;
        private int  [] _prev;
        private int  [] _nextN;
        private int  [] _prevN;
        
        private int _free;   // Unused entries
        private int _naked;  // Entries without Curs
    }

    void push ( )
    {
        assert isPositioned();

        int i = _locale._locations.allocate( this );
        _stackTop = _locale._locations.insert( _stackTop, _stackTop, i );
    }

    void popButStay ( )
    {
        if (_stackTop != Locations.NULL)
            _stackTop = _locale._locations.remove( _stackTop, _stackTop );
    }
    
    boolean pop ( )
    {
        if (_stackTop == Locations.NULL)
            return false;

        _locale._locations.moveTo( _stackTop, this );
        _stackTop = _locale._locations.remove( _stackTop, _stackTop );

        return true;
    }

    boolean isAtLastPush ( )
    {
        assert _stackTop != Locations.NULL;
        
        return _locale._locations.isSamePos( _stackTop, this );
    }
    
    boolean isAtEndOfLastPush ( )
    {
        assert _stackTop != Locations.NULL;
        
        return _locale._locations.isAtEndOf( _stackTop, this );
    }

    void addToSelection ( Cur that )
    {
        assert that != null && that.isNormal();
        assert isPositioned() && that.isPositioned();

        int i = _locale._locations.allocate( that );
        _selectionFirst = _locale._locations.insert( _selectionFirst, Locations.NULL, i );
        
        _selectionCount++;
    }
    
    void addToSelection ( )
    {
        assert isPositioned();

        int i = _locale._locations.allocate( this );
        _selectionFirst = _locale._locations.insert( _selectionFirst, Locations.NULL, i );
        
        _selectionCount++;
    }

    private int selectionIndex ( int i )
    {
        assert _selectionN >= -1 && i >= 0 && i < _selectionCount;
        
        if (_selectionN == -1)
        {
            _selectionN = 0;
            _selectionLoc = _selectionFirst;
        }

        while ( _selectionN < i )
        {
            _selectionLoc = _locale._locations.next( _selectionLoc );
            _selectionN++;
        }
            
        while ( _selectionN > i )
        {
            _selectionLoc = _locale._locations.prev( _selectionLoc );
            _selectionN--;
        }

        return _selectionLoc;
    }
    
    void removeSelection ( int i )
    {
        assert i >= 0 && i < _selectionCount;

        int j = selectionIndex( i );

        // Update the nth selection indices to accomodate the deletion
        
        if (i < _selectionN)
            _selectionN--;
        else if (i == _selectionN)
        {
            _selectionN--;
            
            if (i == 0)
                _selectionLoc = Locations.NULL;
            else
                _selectionLoc = _locale._locations.prev( _selectionLoc );
        }

        _selectionFirst = _locale._locations.remove( _selectionFirst, j );
        
        _selectionCount--;
    }

    int selectionCount ( )
    {
        return _selectionCount;
    }

    void moveToSelection ( int i )
    {
        assert i >= 0 && i < _selectionCount;

        _locale._locations.moveTo( selectionIndex( i ), this );
    }

    void clearSelection ( )
    {
        assert _selectionCount >= 0;
        
        while ( _selectionCount > 0 )
            removeSelection( 0 );
    }

    boolean toParent    ( ) { return toParent( false ); }
    boolean toParentRaw ( ) { return toParent( true  ); }
    
    Xobj getParent    ( ) { return getParent( false ); }
    Xobj getParentRaw ( ) { return getParent( true  ); }
    
    boolean hasParent ( )
    {
        assert isPositioned();

        if (_pos == END_POS || (_pos >= 1 && _pos < _xobj.posAfter()))
            return true;

        assert _pos == 0 || _xobj._parent != null;
        
        return _xobj._parent != null;
    }
    
    Xobj getParent ( boolean raw )
    {
        assert isPositioned();

        if (_pos == END_POS || (_pos >= 1 && _pos < _xobj.posAfter()))
            return _xobj;

        assert _pos == 0 || _xobj._parent != null;

        if (_xobj._parent != null)
            return _xobj._parent;
        
        if (raw || _xobj.isRoot())
            return null;

        Cur r = _locale.tempCur();
        
        r.createRoot();

        Xobj root = r._xobj;
        
        r.next();
        moveNode( r );
        r.release();

        return root;
    }
    
    boolean toParent ( boolean raw )
    {
        Xobj parent = getParent( raw );

        if (parent == null)
            return false;

        moveTo( parent );

        return true;
    }

    boolean hasText ( )
    {
        assert isNode();
        
        return _xobj.hasTextEnsureOccupancy();
    }

    boolean hasAttrs ( )
    {
        assert isNode();
        
        return _xobj.hasAttrs();
    }
    
    boolean hasChildren ( )
    {
        assert isNode();
        
        return _xobj.hasChildren();
    }
    
    boolean toFirstChild ( )
    {
        assert isNode();

        if (!_xobj.hasChildren())
            return false;

        for ( Xobj x = _xobj._firstChild ; ; x = x._nextSibling )
        {
            if (!x.isAttr())
            {
                moveTo( x );
                return true;
            }
        }
    }
    
    protected boolean toLastChild ( )
    {
        assert isNode();

        if (!_xobj.hasChildren())
            return false;

        moveTo( _xobj._lastChild );

        return true;
    }

    boolean toNextSibling ( )
    {
        assert isNode();

        if (_xobj.isAttr())
        {
            if (_xobj._nextSibling != null && _xobj._nextSibling.isAttr())
            {
                moveTo( _xobj._nextSibling );
                return true;
            }
        }
        else if (_xobj._nextSibling != null)
        {
            moveTo( _xobj._nextSibling );
            return true;
        }

        return false;
    }

    String getAttrValue ( QName name )
    {
        String s = null;
        
        push();

        if (toAttr( name ))
            s = getValueAsString();
        
        pop();

        return s;
    }

    void setValueAsQName ( QName qname )
    {
        assert isNode();

        String value  = qname.getLocalPart();
        String ns     = qname.getNamespaceURI();
        
        String prefix =
            prefixForNamespace(
                ns, qname.getPrefix().length() > 0 ? qname.getPrefix() : null, true );

        if (prefix.length() > 0)
            value = prefix + ":" + value;

        setValue( value );
    }

    void setValue ( String value )
    {
        assert isNode();
        
        moveNodeContents( null, false );
        
        next();
        
        insertString( value );

        toParent();
    }

    void removeFollowingAttrs ( )
    {
        assert isAttr();
        
        QName attrName = getName();

        push();
        
        if (toNextAttr())
        {
            while ( isAttr() )
            {
                if (getName().equals( attrName ))
                    moveNode( null );
                else if (!toNextAttr())
                    break;
            }
        }

        pop();
    }

    void setAttrAsQName ( QName name, QName value )
    {
        assert isContainer();

        if (toAttr( name ))
            removeFollowingAttrs();
        else
        {
            next();
            createAttr( name );
        }
        
        setValueAsQName( value );

        toParent();
    }
    
    boolean removeAttr ( QName name )
    {
        assert isContainer();

        return _xobj.removeAttr( name );
    }

    void setAttr ( QName name, String value )
    {
        assert isContainer();

        _xobj.setAttr( name, value );
    }
    
    boolean toAttr ( QName name )
    {
        assert isNode();

        Xobj a = _xobj.getAttr( name );

        if (a == null)
            return false;

        moveTo( a );

        return true;
    }

    boolean toFirstAttr ( )
    {
        assert isNode();

        Xobj firstAttr = _xobj.firstAttr();

        if (firstAttr == null)
            return false;

        moveTo( firstAttr );

        return true;
    }
    
    boolean toLastAttr ( )
    {
        assert isNode();

        if (!toFirstAttr())
            return false;

        while ( toNextAttr() )
            ;

        return true;
    }
    
    boolean toNextAttr ( )
    {
        assert isAttr();

        Xobj nextAttr = _xobj.nextAttr();

        if (nextAttr == null)
            return false;
        
        moveTo( nextAttr );

        return true;
    }
    
    boolean toPrevAttr ( )
    {
        assert isAttr();
        
        if (_xobj._prevSibling == null || !_xobj._prevSibling.isAttr())
            return false;
        
        moveTo( _xobj._prevSibling );

        return true;
    }

    boolean skip ( )
    {
        assert isNode();

        if (_xobj.isRoot())
            return false;
        
        if (_xobj.isAttr())
        {
            if (_xobj._nextSibling == null || !_xobj._nextSibling.isAttr())
                return false;

            moveTo( _xobj._nextSibling, 0 );
        }
        else
            moveTo( getNormal( _xobj, _xobj.posAfter() ), _posTemp );

        return true;
    }
    
    void toEnd ( )
    {
        assert isNode();
        
        moveTo( _xobj, END_POS );
    }
    
    void moveToCharNode ( CharNode node )
    {
        assert node.getDom() != null && node.getDom().locale() == _locale;

        moveToDom( node.getDom() );

        CharNode n;

        _xobj.ensureOccupancy();
        
        n = _xobj._charNodesValue =
            updateCharNodes( _locale, _xobj, _xobj._charNodesValue, _xobj._cchValue );
        
        for ( ; n != null ; n = n._next )
        {
            if (node == n)
            {
                moveTo( getNormal( _xobj, n._off + 1 ), _posTemp );
                return;
            }
        }

        n = _xobj._charNodesAfter =
            updateCharNodes( _locale, _xobj, _xobj._charNodesAfter, _xobj._cchAfter );

        for ( ; n != null ; n = n._next )
        {
            if (node == n)
            {
                moveTo( getNormal( _xobj, n._off + _xobj._cchValue + 2 ), _posTemp );
                return;
            }
        }

        assert false;
    }
    
    boolean prev ( )
    {
        assert isPositioned();

        if (_xobj.isRoot() && _pos == 0)
            return false;
        
        Xobj x = getDenormal();
        int  p = _posTemp;

        assert p > 0 && p != END_POS;

        int pa = x.posAfter();

        if (p > pa)
            p = pa;
        else if (p == pa)
        {
            // Text after an attr is allowed only on the last attr,
            // and that text belongs to the parent container..  
            //
            // If we're a thte end of the last attr, then we were just
            // inside the container, and we need to skip the attrs.
            
            if (x.isAttr() &&
                (x._cchAfter > 0 || x._nextSibling == null || !x._nextSibling.isAttr()))
            {
                x = x.ensureParent();
                p = 0;
            }
            else
                p = END_POS;
        }
        else if (p == pa - 1)
        {
            x.ensureOccupancy();
            p = x._cchValue > 0 ? 1 : 0;
        }
        else if (p > 1)
            p = 1;
        else
        {
            assert p == 1;
            p = 0;
        }
        
        moveTo( getNormal( x, p ), _posTemp );

        return true;
    }
    
    boolean next ( boolean withAttrs )
    {
        return withAttrs ? nextWithAttrs() : next();
    }
    
    boolean nextWithAttrs ( )
    {
        int k = kind();

        if (kindIsContainer( k ))
        {
            if (toFirstAttr())
                return true;
        }
        else if (k == -ATTR)
        {
            if (next())
                return true;
            
            toParent();
            
            if (!toParentRaw())
                return false;
        }
        
        return next();
    }
    
    boolean next ( )
    {
        assert isNormal();

        Xobj x = _xobj;
        int  p = _pos;

        int pa = x.posAfter();

        if (p >= pa)
            p = _xobj.posMax();
        else if (p == END_POS)
        {
            if (x.isRoot() || (x.isAttr() && (x._nextSibling == null || !x._nextSibling.isAttr())))
                return false;
            
            p = pa;
        }
        else if (p > 0)
        {
            assert x._firstChild == null || !x._firstChild.isAttr();

            if (x._firstChild != null)
            {
                x = x._firstChild;
                p = 0;
            }
            else
                p = END_POS;
        }
        else
        {
            assert p == 0;

            x.ensureOccupancy();
            
            p = 1;
            
            if (x._cchValue == 0)
            {
                if (x._firstChild != null)
                {
                    if (x._firstChild.isAttr())
                    {
                        Xobj a = x._firstChild;

                        while ( a._nextSibling != null && a._nextSibling.isAttr() )
                            a = a._nextSibling;

                        if (a._cchAfter > 0)
                        {
                            x = a;
                            p = a.posAfter();
                        }
                        else if (a._nextSibling != null)
                        {
                            x = a._nextSibling;
                            p = 0;
                        }
                    }
                    else
                    {
                        x = x._firstChild;
                        p = 0;
                    }
                }
            }
        }

        moveTo( getNormal( x, p ), _posTemp );

        return true;
    }

    int prevChars ( int cch )
    {
        assert isPositioned();

        int cchLeft = cchLeft();

        if (cch < 0 || cch > cchLeft)
            cch = cchLeft;

        if (cch == 0)
            return 0;

        _pos -= cch;

        return cch;
    }
    
    int nextChars ( int cch )
    {
        assert isPositioned();
        
        int cchRight = cchRight();

        if (cchRight == 0)
            return 0;

        if (cch < 0 || cch >= cchRight)
        {
            // Use next to not skip over children
            next();
            return cchRight;
        }

        moveTo( getNormal( _xobj, _pos + cch ), _posTemp );
        
        return cch;
    }

    void setCharNodes ( CharNode nodes )
    {
        assert nodes == null || _locale == nodes.locale();
        assert isPositioned();
        
        Xobj x = getDenormal();
        int  p = _posTemp;
        
        assert !x.isRoot() || (p > 0 && p < x.posAfter());

        if (p >= x.posAfter())
            x._charNodesAfter = nodes;
        else
            x._charNodesValue = nodes;

        for ( ; nodes != null ; nodes = nodes._next )
            nodes.setDom( (Dom) x );

        // No Need to notify text change or alter version, text nodes are
        // not part of the infoset
    }

    CharNode getCharNodes ( )
    {
        assert isPositioned();
        assert !isRoot();
        
        Xobj x = getDenormal();

        CharNode nodes;

        if (_posTemp >= x.posAfter())
        {
            nodes = x._charNodesAfter =
                updateCharNodes( _locale, x, x._charNodesAfter, x._cchAfter );
        }
        else
        {
            x.ensureOccupancy();
            
            nodes = x._charNodesValue =
                updateCharNodes( _locale, x, x._charNodesValue, x._cchValue );
        }

        return nodes;
    }
    
    private static CharNode updateCharNodes ( Locale l, Xobj x, CharNode nodes, int cch )
    {
        assert nodes == null || nodes.locale() == l;
        
        CharNode node = nodes;
        int i = 0;

        while ( node != null && cch > 0 )
        {
            assert node.getDom() == x;

            if (node._cch > cch)
                node._cch = cch;

            node._off = i;
            i += node._cch;
            cch -= node._cch;

            node = node._next;
        }

        if (cch <= 0)
        {
            for ( ; node != null ; node = node._next )
            {
                assert node.getDom() == x;

                if (node._cch != 0)
                    node._cch = 0;

                node._off = i;
            }
        }
        else
        {
            node = l.createTextNode();
            node.setDom( (Dom) x );
            node._cch = cch;
            node._off = i;
            nodes = CharNode.appendNode( nodes, node );
        }

        return nodes;
    }

    final QName getXsiTypeName ( )
    {
        assert isNode();

        return _xobj.getXsiTypeName();
    }
    
    final QName valueAsQName ( )
    {
        throw new RuntimeException( "Not implemented" );
    }
    
    final String namespaceForPrefix ( String prefix, boolean defaultAlwaysMapped )
    {
        assert isContainer();

        return _xobj.namespaceForPrefix( prefix, defaultAlwaysMapped );
    }

    final String prefixForNamespace ( String ns, String suggestion, boolean createIfMissing )
    {
        if (ns == null)
            ns = "";

        // special cases
        
        if (ns.equals( Locale._xml1998Uri ))
            return "xml";
        
        if (ns.equals( Locale._xmlnsUri ))
            return "xmlns";

        // Get the closest container for the spot we're on

        Xobj base = isContainer() ? _xobj : getParent();

        // Special handling for the no-namespace case
        
        if (ns.length() == 0)
        {
            // Search for a namespace decl which defines the default namespace

            Xobj a = base.findXmlnsForPrefix( "" );

            // If I did not find a default decl or the decl maps to the no namespace, then
            // the default namespace is mapped to ""
            
            if (a == null || a.getXmlnsUri().length() == 0)
                return "";

            // At this point, I've found a default namespace which is *not* the no-namespace.
            // If I can't modify the document to mape the desired no-namespace, I must fail.
            
            if (!createIfMissing)
                return null;

            // Ok, I need to make the default namespace on the nearest container map to ""

            base.setAttr( _locale.createXmlns( null ), "" );
            
            return "";
        }

        // Look for an exisiting mapping for the desired uri which has a visible prefix 

        for ( Xobj c = base ; c != null ; c = c._parent )
            for ( Xobj a = c.firstAttr() ; a != null ; a = a.nextAttr() )
                if (a.isXmlns() && a.getXmlnsUri().equals( ns ))
                    if (base.findXmlnsForPrefix( a.getXmlnsPrefix() ) == a)
                        return a.getXmlnsPrefix();

        // No exisiting xmlns I can use, need to create one.  See if I can first

        if (!createIfMissing)
            return null;

        // Sanitize the suggestion.

        if (suggestion != null &&
              (suggestion.length() == 0 || suggestion.toLowerCase().startsWith( "xml" ) ||
                    base.findXmlnsForPrefix( suggestion ) != null))
        {
            suggestion = null;
        }

        // If no suggestion, make one up

        if (suggestion == null)
        {
            String prefixBase = QNameHelper.suggestPrefix( ns );
            
            suggestion = prefixBase;
            
            for ( int i = 1 ; ; suggestion = prefixBase + i++ )
                if (base.findXmlnsForPrefix( suggestion ) == null)
                    break;
        }

        // Add a new namespace decl at the top elem if one exists, otherwise at root

        Xobj c = base;

        while ( !c.isRoot() && !c.ensureParent().isRoot() )
            c = c._parent;
        
        base.setAttr( _locale.createXmlns( suggestion ), ns );

        return suggestion;
    }

    // Does the node at this cursor properly contain the position specified by the argument

    boolean contains ( Cur that )
    {
        assert isNode();
        assert that != null && that.isPositioned();

        return _xobj.contains( that );
    }

    void insertString ( String s )
    {
        insertChars( s, 0, s.length() );
    }
    
    void insertChars ( Object src, int off, int cch )
    {
        assert isPositioned() && !isRoot();
        assert CharUtil.isValid( src, off, cch );

        // Check for nothing to insert

        if (cch <= 0)
            return;

        _locale.notifyChange();

        // The only situation where I need to ensure occupancy is when I'm at the end of a node.
        // All other positions will require occupancy.  For example, if I'm at the beginning of a
        // node, then I will either insert in the after text of the previous sibling, or I will
        // insert in the value of the parent.  In the latter case, because the parent has a child,
        // it cannot be vacant.
        
        if (_pos == END_POS)
            _xobj.ensureOccupancy();

        // Get the denormailized Xobj and pos.  This is the Xobj which will actually receive
        // the new chars.  Note that a denormalized position can never be <= 0.
        
        Xobj x = getDenormal();
        int  p = _posTemp;
        
        assert p > 0;

        // This will move "this" cursor to be after the inserted text. No worries, I'll update its
        // position after.  This insertChars takes care of all the appropriate invalidations
        // (passing true as last arg).
        
        x.insertCharsHelper( p, src, off, cch, true );

        // Reposition the cursor to be just before the newly inserted text.  It's current
        // position could have been shifted, or it may have been just before the end tag, or
        // normalized on another Xobj.
        
        moveTo( x, p );

        _locale._versionAll++;
    }

    // Move the chars just after this Cur to the "to" Cur.  If no "to" Cur is specified,
    // then remove the chars.
    
    Object moveChars ( Cur to, int cchMove )
    {
        assert isPositioned();
        assert cchMove <= 0 || cchMove <= cchRight();
        assert to == null || (to.isPositioned() && !to.isRoot());

        if (cchMove < 0)
            cchMove = cchRight();

        // If we're instructed to move 0 characters, then return the null triple.
        
        if (cchMove == 0)
        {
            _offSrc = 0;
            _cchSrc = 0;
            
            return null;
        }

        // Here I record the triple of the chars to move.  I will return this.  No need to save
        // cch 'cause cchMove will be that value.

        Object srcMoved = getChars( cchMove );
        int    offMoved = _offSrc;

        // Either I'm moving text from the value or the after text.  If after, then the container
        // must be occupied.  If in the value, because we're just before text, it must be occupied.

        assert isText() && (_pos >= _xobj.posAfter() ? _xobj._parent : _xobj).isOccupied();
                    
        if (to == null)
        {
            // In this case, I'm removing chars vs moving them.  Normally I would like to blow
            // them away entirely, but if there are any references to those chars via a bookmark
            // I need to keep them alive.  I do this by moving these chars to a new root.  Note
            // that because Curs will stay behind, I don't have to check for them.
            
            for ( Bookmark b = _xobj._bookmarks ; b != null ; b = b._next )
            {
                if (inChars( b, cchMove, false ))
                {
                    Cur c = _locale.tempCur();

                    c.createRoot();
                    c.next();

                    Object chars = moveChars( c, cchMove );

                    c.release();

                    return chars;
                }
            }
        }
        else
        {
            // If the target, "to", is inside or on the edge of the text to be moved, then this
            // is a no-op.  In this case, I still want to return the text "moved".
            //
            // Note how I move "to" and this cur around.  I move "to" to be at the beginning of the
            // chars moved and "this" to be at the end.  If the text were really moving to a
            // different location, then "to" would be at the beginning of the newly moved chars,
            // and "this" would be at the gap left by the newly removed chars.  
            
            if (inChars( to, cchMove, true ))
            {
                // BUGBUG - may want to consider shuffling the interior cursors to the right just
                // like I move "this" to the right...
                
                to.moveToCur( this );
                nextChars( cchMove );
                
                _offSrc = offMoved;
                _cchSrc = cchMove;
                
                return srcMoved;
            }

            // Copy the chars here, I'll remove the originals next

            to.insertChars( srcMoved, offMoved, cchMove );
        }

        // Notice that I can delay the general change notification to this point because any
        // modifications up to this point are made by calling other high level operations which
        // generate this notification themselves.  Also, no need to notify of general change in
        // the "to" locale because the insertion of chars above handles that.

        _locale.notifyChange();

        // Create a cursor to be where "this" will be after the move.  I need to do this because
        // the removal of the chars can confuse where "this" is.  Consider the case where chars
        // are being removed from just before the end of a tag.  In this case, the pos will be
        // just before the end of the tag after the move and normalizing this position will result
        // in teh cur going to just before the end tag.  However, if there were children, this
        // needs to move there.

        if (to == null)
            _xobj.removeCharsHelper( _pos, cchMove, null, NO_POS, false, true );
        else
            _xobj.removeCharsHelper( _pos, cchMove, to._xobj, to._pos, false, true );

        // Need to update the position of this cursor even though it did not move anywhere.  This
        // needs to happen because it may not be properly normalized anymore.  Note that because
        // of the removal of the text, this cur may not be normal any more, thus I call moveTo
        // which does not assume this.

        _locale._versionAll++;

        _offSrc = offMoved;
        _cchSrc = cchMove;

        return srcMoved;
    }

    void moveNode ( Cur to )
    {
        assert isNode() && !isRoot();
        assert to == null || to.isPositioned();
        assert to == null || !contains( to );
        assert to == null || !to.isRoot();
        
        // TODO - should assert that is an attr is being moved, it is ok there


        // Record the node to move and skip this cur past it.  This moves this cur to be after
        // the move to move/remove -- it's final resting place.  The only piece of information
        // about the source of the move is the node itself.

        Xobj x = _xobj;

        skip();

        // I call another function here to move the node.  I do this because  I don't have to
        // worry about messing with "this" here given that it not should be treated like any other
        // cursor after this point.

        moveNode( x, to );
    }

    // Moves text from one place to another in a low-level way, used as a helper for the higher
    // level functions.  Takes care of moving bookmarks and cursors.  In the high level content
    // manipulation functions, cursors do not follow content, but this helper moves them.  The
    // arguments are denormalized.  The Xobj's must be different from eachother but from the same
    // locale.  The destination must not be not be vacant.

    private static void transferChars ( Xobj xFrom, int pFrom, Xobj xTo, int pTo, int cch )
    {
        assert xFrom != xTo;
        assert xFrom._locale == xTo._locale;
        assert pFrom > 0 && pFrom <  xFrom.posMax();
        assert pTo   > 0 && pTo   <= xTo  .posMax();
        assert cch > 0 && cch <= xFrom.cchRight( pFrom );
        assert pTo >= xTo.posAfter() || xTo.isOccupied();

        // Copy the chars from -> to without performing any invalidations.  This will scoot curs
        // and marks around appropriately.  Note that I get the cars with getCharsHelper which
        // does not check for normalization because the state of the tree at this moment may not
        // exactly be "correct" here.

        xTo.insertCharsHelper(
            pTo, xFrom.getCharsHelper( pFrom, cch ), xFrom.offSrc(), xFrom.cchSrc(), false );
        
        xFrom.removeCharsHelper( pFrom, cch, xTo, pTo, true, false );
    }

    // Moves the node x to "to", or removes it if to is null.

    static void moveNode ( Xobj x, Cur to )
    {
        assert x != null && !x.isRoot();
        assert to == null || to.isPositioned();
        assert to == null || !x.contains( to );
        assert to == null || !to.isRoot();

        if (to != null)
        {
            // Before I go much further, I want to make sure that if "to" is in the container of
            // a vacant node, I get it occupied.  I do not need to worry about the source being
            // vacant.

            if (to._pos == END_POS)
                to._xobj.ensureOccupancy();

            // See if the destination is on the edge of the node to be moved (a no-op).  It is
            // illegal to call this fcn when to is contained within the node to be moved.  Note
            // that I make sure that to gets oved to the beginning of the node.  The position of
            // to in all operations should leave to just before the content moved/inserted.

            if ((to._pos == 0 && to._xobj == x) || to.isJustAfterEnd( x ))
            {
                // TODO - should shuffle contained curs to the right???
                
                to.moveTo( x );
                return;
            }
        }

        // Notify the locale(s) about the change I am about to make.

        x._locale.notifyChange();
        
        x._locale._versionAll++;
        x._locale._versionSansText++;

        if (to != null && to._locale != x._locale)
        {
            to._locale.notifyChange();
            
            to._locale._versionAll++;
            to._locale._versionSansText++;
        }

        // Node is going away.  Invalidate the parent (the text around the node is merging).
        // Also, this node may be an attribute -- invalidate special attrs ...

        if (x.isAttr())
            x.invalidateSpecialAttr( to == null ? null : to.getParentRaw() );
        else
        {
            if (x._parent != null)
                x._parent.invalidateUser();
            
            if (to != null && to.hasParent())
                to.getParent().invalidateUser();
        }

        // If there is any text after x, I move it to be before x.  This frees me to extract x
        // and it's contents with out this text coming along for the ride.  Note that if this
        // node is the last attr and there is text after it, transferText will move the text
        // to a potential previous attr.  This is an invalid state for a short period of time.
        // I need to move this text away here so that when I walk the tree next, *all* curs
        // embedded in this node or deeper will be moved off this node.

        if (x._cchAfter > 0)
            transferChars( x, x.posAfter(), x.getDenormal( 0 ), x.posTemp(), x._cchAfter );

        assert x._cchAfter == 0;

        // Walk the node tree, moving curs out, disconnecting users and relocating to a, possibly,
        // new locale.  I embed the cursors in this locale before itersting to just cause the
        // embed to happen once.

        x._locale.embedCurs();

        for ( Xobj y = x ; y != null ; y = y.walk( x ) )
        {
            while ( y._embedded != null )
                y._embedded.moveTo( x.getNormal( x.posAfter() ) );

            y.disconnectUser();

            if (to != null)
                y._locale = to._locale;
        }

        // Now, actually remove the node

        x.removeXobj();

        // Now, if there is a destination, insert the node there and shuffle the text in the
        // vicinity of the destination appropriately.

        if (to != null)
        {
            // To know where I should insert/append the node to move, I need to see where "to"
            // would be if there were no text after it.  However, I need to keep "to" where it
            // is when I move the text after it later.
            
            Xobj here = to._xobj;
            boolean append = to._pos != 0;

            int cchRight = to.cchRight();
            
            if (cchRight > 0)
            {
                to.push();
                to.next();
                here = to._xobj;
                append = to._pos != 0;
                to.pop();
            }

            if (append)
                here.appendXobj( x );
            else
                here.insertXobj( x );

            // The only text I need to move is that to the right of "to".  Even considering all
            // the cases where an attribute is involed!

            if (cchRight > 0)
                transferChars( to._xobj, to._pos, x, x.posAfter(), cchRight );
            
            to.moveTo( x );
        }
    }

    void moveNodeContents ( Cur to, boolean moveAttrs )
    {
        assert isNode();
        assert to == null || !to.isRoot();

        // By calling this helper, I do not have to deal with this Cur any longer.  Basically,
        // this Cur is out of the picture, it behaves like any other cur at this point.

        moveNodeContents( _xobj, to, moveAttrs );
    }

    private static void moveNodeContents ( Xobj x, Cur to, boolean moveAttrs )
    {
        // TODO - should assert that is an attr is being moved, it is ok there
        
        assert to == null || !to.isRoot();

        // Collect a bit of information about the contents to move first.  Note that the collection
        // of this info must not cause a vacant value to become occupied.

        boolean hasAttrs = x.hasAttrs();
        boolean noSubNodesToMove = !x.hasChildren() && (!moveAttrs || !hasAttrs);

        // Deal with the cases where only text is involved in the move

        if (noSubNodesToMove)
        {
            // If we're vacant and there is no place to move a potential value, then I can avoid
            // acquiring the text from the TypeStoreUser.  Otherwise, there may be text here I
            // need to move somewhere else.
            
            if (x.isVacant() && to == null)
            {
                x.clearBit( Xobj.VACANT );

                x.invalidateUser();
                x.invalidateSpecialAttr( null );
                x._locale._versionAll++;
            }
            else if (x.hasTextEnsureOccupancy())
            {
                Cur c = x.tempCur();
                c.next();
                c.moveChars( to, -1 );
                c.release();
            }
            
            return;
        }

        // Here I check to see if "to" is just inside x.  In this case this is a no-op.  Note that
        // the value of x may still be vacant.
        
        if (to != null)
        {
            // Quick check of the right edge.  If it is there, I need to move "to" to the left edge
            // so that it is positioned at the beginning of the "moved" content.
            
            if (x == to._xobj && to._pos == END_POS)
            {
                // TODO - shuffle interior curs?
                
                to.moveTo( x );
                to.next( moveAttrs && hasAttrs );
                
                return;
            }

            // Here I need to see if to is at the left edge.  I push to's current position and
            // then navigate it to the left edge then compare it to the pushed position...

            to.push();
            to.moveTo( x );
            to.next( moveAttrs && hasAttrs );
            boolean isSame = to.isAtLastPush();
            to.pop();
            
            // TODO - shuffle interior curs?
            
            if (isSame)
                return;

            // Now, after dealing with the edge condition, I can assert that to is not inside x

            assert !x.contains( to );
            
            // So, at this point, I've taken case of the no-op cases and the movement of just text.
            // Also, to must be occupied because I took care of the text only and nothing to move
            // cases.

            assert to.getParent().isOccupied();
        }

        // TODO - did I forget to put a changeNotification here?  Look more closely ...

        // Deal with the value text of x which is either on x or the last attribute of x.
        // I need to get it out of the way to properly deal with the walk of the contents.
        // In order to reposition "to" properly later, I need to record how many chars were moved.

        int valueMovedCch = 0;

        if (x.hasTextNoEnsureOccupancy())
        {
            Cur c = x.tempCur();
            c.next();
            c.moveChars( to, -1 );
            c.release();

            if (to != null)
                to.nextChars( valueMovedCch = c._cchSrc );
        }

        // Now, walk all the contents, invalidating special attrs, reportioning cursors,
        // disconnecting users and relocating to a potentially different locale.  Because I moved
        // the value text above, no top level attrs should have any text.

        x._locale.embedCurs();
        
        Xobj firstToMove = x.walk( x );
        boolean sawBookmark = false;

        for ( Xobj y = firstToMove ; y != null ; y = y.walk( x ) )
        {
            if (y._parent == x && y.isAttr())
            {
                assert y._cchAfter == 0;
                
                if (!moveAttrs)
                {
                    firstToMove = y._nextSibling;
                    continue;
                }

                y.invalidateSpecialAttr( to == null ? null : to.getParent() );
            }
            
            for ( Cur c ; (c = y._embedded) != null ; )
                c.moveTo( x, END_POS );
            
            y.disconnectUser();

            if (to != null)
                y._locale = to._locale;

            sawBookmark = sawBookmark || y._bookmarks != null;
        }

        Xobj lastToMove = x._lastChild;

        // If there were any bookmarks in the tree to remove, to preserve the content that these
        // bookmarks reference, move the contents to a new root.  Note that I already moved the
        // first piece of text above elsewhere.  Note: this has the effect of keeping all of the
        // contents alive even if there is one bookmark deep into the tree.  I should really
        // disband all the content, except for the pieces which are bookmarked.

        Cur surragateTo = null;
        
        if (sawBookmark && to == null)
        {
            surragateTo = to = x._locale.tempCur();
            to.createRoot();
            to.next();
        }

        // Perform the rest of the invalidations.  If only attrs are moving, then no user
        // invalidation needed.  If I've move text to "to" already, no need to invalidate
        // again.

        if (!lastToMove.isAttr())
            x.invalidateUser();

        x._locale._versionAll++;
        x._locale._versionSansText++;

        if (to != null && valueMovedCch == 0)
        {
            to.getParent().invalidateUser();
            to._locale._versionAll++;
            to._locale._versionSansText++;
        }

        // Remove the children and, if needed, move them

        x.removeXobjs( firstToMove, lastToMove );

        // To know where I should insert/append the contents to move, I need to see where "to"
        // would be if there were no text after it.  

        Xobj here = to._xobj;
        boolean append = to._pos != 0;

        int cchRight = to.cchRight();

        if (cchRight > 0)
        {
            to.push();
            to.next();
            here = to._xobj;
            append = to._pos != 0;
            to.pop();
        }

        // Now, I have to shuffle the text around "to" in special ways.  A complication is
        // the insertion of attributes.  First, if I'm inserting attrs here then, logically,
        // there can be no text to the left because attrs can only live after another attr
        // or just inside a container.  So, If attrs are being inserted and there is value
        // text on the target container, I will need to move this value text to be after
        // the lew last attribute.  Note that this value text may already live on a current
        // last attr (before the inserting).  Also, I need to figure this all out before I
        // move the text after "to" because this text may end up being sent to the same place
        // as the containers value text when the last new node being inserted is an attr!
        // Whew!

        if (firstToMove.isAttr())
        {
            Xobj lastNewAttr = firstToMove;

            while ( lastNewAttr._nextSibling != null && lastNewAttr._nextSibling.isAttr() )
                lastNewAttr = lastNewAttr._nextSibling;

            // Get to's parnet now before I potentially move him with the next transfer

            Xobj y = to.getParent();

            if (cchRight > 0)
                transferChars( to._xobj, to._pos, lastNewAttr, lastNewAttr.posMax(), cchRight );

            if (y.hasTextNoEnsureOccupancy())
            {
                int p, cch;

                if (y._cchValue > 0)
                {
                    p = 1;
                    cch = y._cchValue;
                }
                else
                {
                    y = y.lastAttr();
                    p = y.posAfter();
                    cch = y._cchAfter;
                }

                transferChars( y, p, lastNewAttr, lastNewAttr.posAfter(), cch );
            }
        }
        else if (cchRight > 0)
            transferChars( to._xobj, to._pos, lastToMove, lastToMove.posMax(), cchRight );

        // After mucking with the text, splice the new tree in

        if (append)
            here.appendXobjs( firstToMove, lastToMove );
        else
            here.insertXobjs( firstToMove, lastToMove );

        // Position "to" to be at the beginning of the newly inserted contents

        to.moveTo( firstToMove );
        to.prevChars( valueMovedCch );

        // If I consed up a to, release it here

        if (surragateTo != null)
            surragateTo.release();
    }
    
    protected final void setBookmark ( Object key, Object value )
    {
        assert isNormal();
        assert key != null;

        for ( Bookmark b = _xobj._bookmarks ; b != null ; b = b._next )
        {
            if (_pos == b._pos && key == b._key)
            {
                if (value == null)
                    _xobj._bookmarks = b.listRemove( _xobj._bookmarks );
                else
                    b._value = value;

                return;
            }
        }

        Bookmark b = new Bookmark();

        b._xobj  = _xobj;
        b._pos   = _pos;
        b._key   = key;
        b._value = value;

        _xobj._bookmarks = b.listInsert( _xobj._bookmarks );
    }
    
    final Object getBookmark ( Object key )
    {
        assert isNormal();
        assert key != null;

        for ( Bookmark b = _xobj._bookmarks ; b != null ; b = b._next )
            if (b._pos == _pos && b._key == key)
                return b._value;
        
        return null;
    }
    
    String getString ( int cch )
    {
        assert isNormal() && _xobj != null;

        return _xobj.getString( _pos, cch, Locale.WS_PRESERVE );
    }

    String getString ( int cch, int wsr )
    {
        assert isNormal() && _xobj != null;

        return _xobj.getString( _pos, cch, wsr );
    }

    String getValueAsString ( int wsr )
    {
        assert isNode();

        // TODO - make sure there are no children (ok for an element to have
        // attrs)

        assert ! hasChildren();

        return _xobj.getValue( wsr );
    }
    
    String getValueAsString ( )
    {
        return getValueAsString( Locale.WS_PRESERVE );
    }

    Object getChars ( int cch )
    {
        assert isPositioned();

        return _xobj.getChars( _pos, cch, this );
    }
    
    Object getValueChars ( )
    {
        assert isNode();
        
        _xobj.ensureOccupancy();
        
        return _xobj.getChars( 1, -1, this );
    }
    
    void copyNode ( Cur to )
    {
        assert to != null;
        assert isNode();

        Xobj newParent = null;
        Xobj copy = null;
            
        walk:
        for ( Xobj x = _xobj ; ; )
        {
            x.ensureOccupancy();
            
            Xobj newX = x.newNode( to._locale );

            newX._srcValue = x._srcValue;
            newX._offValue = x._offValue;
            newX._cchValue = x._cchValue;
            
            newX._srcAfter = x._srcAfter;
            newX._offAfter = x._offAfter;
            newX._cchAfter = x._cchAfter;

            // TODO - strange to have charNode stuff inside here .....
            newX._charNodesValue = CharNode.copyNodes( x._charNodesValue, newX );
            newX._charNodesAfter = CharNode.copyNodes( x._charNodesAfter, newX );

            if (newParent == null)
                copy = newX;
            else
                newParent.appendXobj( newX );

            // Walk to the next in-order xobj.  Record the current (y) to compute newParent

            Xobj y = x;

            if ((x = x.walk( _xobj )) == null)
                break;

            if (y == x._parent)
                newParent = newX;
            else
                for ( ; y._parent != x._parent ; y = y._parent )
                    newParent = newParent._parent;
        }

        copy._srcAfter = null;
        copy._offAfter = 0;
        copy._cchAfter = 0;

        if (to.isPositioned())
        {
            Cur from = to._locale.tempCur();
            from.moveNode( to );
            from.release();
        }
        else
            to.moveTo( copy );
    }

    Cur weakCur ( Object o )
    {
        Cur c = _locale.weakCur( o );
        c.moveToCur( this );
        return c;
    }

    Cur tempCur ( )
    {
        return tempCur( null );
    }
    
    Cur tempCur ( String id )
    {
        Cur c = _locale.tempCur( id );
        c.moveToCur( this );
        return c;
    }

    private Cur tempCur ( Xobj x, int p )
    {
        assert _locale == x._locale;
        assert x != null || p == NO_POS;

        Cur c = _locale.tempCur();

        if (x != null)
            c.moveTo( getNormal( x, p ), _posTemp );
        
        return c;
    }

    // Is a cursor (c) in the chars defined by cch chars after where this Cur is positioned.
    // Is inclusive on the left, and inclusive/exclusive on the right depending on the value
    // of includeEnd.
    
    boolean inChars ( Cur c, int cch, boolean includeEnd )
    {
        assert isPositioned() && isText() && cchRight() >= cch;
        assert c.isNormal();
        
        return _xobj.inChars( _pos, c._xobj, c._pos, cch, includeEnd );
    }

    boolean inChars ( Bookmark b, int cch, boolean includeEnd )
    {
        assert isPositioned() && isText() && cchRight() >= cch;
        assert b._xobj.isNormal( b._pos );
        
        return _xobj.inChars( _pos, b._xobj, b._pos, cch, includeEnd );
    }

    // Can't be static because I need to communicate pos in _posTemp :-(
    // I wish I had multiple return vars ...
    
    private Xobj getNormal ( Xobj x, int p )
    {
        Xobj nx = x.getNormal( p );
        _posTemp = x._locale._posTemp;
        return nx;
    }
    
    private Xobj getDenormal ( )
    {
        assert isPositioned();

        return getDenormal( _xobj, _pos );
    }

    private Xobj getDenormal ( Xobj x, int p )
    {
        Xobj dx = x.getDenormal( p );
        _posTemp = x._locale._posTemp;
        return dx;
    }

    // May throw IllegalArgumentException if can't change the type
    
    void setType ( SchemaType type )
    {
        assert type != null;
        assert isUserNode();

        TypeStoreUser user = peekUser();

        if (user != null && user.get_schema_type() == type)
            return;

        if (isRoot())
        {
            _xobj.setStableType( type );
            return;
        }

        // Gotta get the parent user to make sure this type is ok here

        TypeStoreUser parentUser = _xobj.ensureParent().getUser();

        // One may only set the type of an attribute to its 'natural' type because
        // attributes cannot take advantage of the xsiType attribute.

        if (isAttr())
        {
            if (parentUser.get_attribute_type( getName() ) != type)
            {
                throw
                    new IllegalArgumentException(
                        "Can't set type of attribute to " + type.toString() );
            }

            return;
        }

        assert isElem();

        // First check to see if this type can be here sans xsi:type.
        // If so, make sure there is no xsi:type

        if (parentUser.get_element_type( getName(), null ) == type)
        {
            removeAttr( Locale._xsiType );
            return;
        }

        // If the desired type has no name, then it cannot be
        // referenced via xsi:type

        QName typeName = type.getName();

        if (typeName == null)
            throw new IllegalArgumentException( "Can't set type of element, type is un-named" );

        // See if setting xsiType would result in the target type
        
        if (parentUser.get_element_type( getName(), typeName ) != type)
            throw new IllegalArgumentException( "Can't set type of element, invalid type" );

        setAttrAsQName( Locale._xsiType, typeName );
    }

    TypeStoreUser peekUser ( )
    {
        assert isUserNode();
        
        return _xobj._user;
    }
    
    TypeStoreUser getUser ( )
    {
        assert isUserNode();

        return _xobj.getUser();
    }

    Dom getDom ( )
    {
        assert isNormal();
        assert isPositioned();

        if (isText())
        {
            int cch = cchLeft();

            for ( CharNode cn = getCharNodes() ; ; cn = cn._next )
                if ((cch -= cn._cch) < 0)
                    return cn;
        }

        return _xobj.getDom();
    }

    static void release ( Cur c )
    {
        if (c != null)
            c.release();
    }

    void release ( )
    {
        if (_state == POOLED || _state == DISPOSED)
            return;

        moveToCur( null );

        assert isNormal();

        assert _xobj == null;
        assert _pos  == NO_POS;

        if (_value instanceof Locale.Ref)
            ((Locale.Ref) _value).clear();

        _value = null;
        _key = null;

        assert _state == REGISTERED;
        _locale._registered = listRemove( _locale._registered );

        if (_locale._curPoolCount < 16)
        {
            _locale._curPool = listInsert( _locale._curPool );
            _state = POOLED;
            _locale._curPoolCount++;
        }
        else
        {
            _locale = null;
            _state = DISPOSED;
        }

        while ( _stackTop != -1 )
            popButStay();

        clearSelection();

        _id = null;
    }

    boolean isOnList ( Cur head )
    {
        for ( ; head != null ; head = head._next )
            if (head == this)
                return true;

        return false;
    }

    Cur listInsert ( Cur head )
    {
        assert _next == null && _prev == null;

        if (head == null)
            head = _prev = this;
        else
        {
            _prev = head._prev;
            head._prev = head._prev._next = this;
        }

        return head;
    }

    Cur listRemove ( Cur head )
    {
        assert _prev != null && isOnList( head );

        if (_prev == this)
            head = null;
        else
        {
            if (head == this)
                head = _next;
            else
                _prev._next = _next;

            if (_next == null)
                head._prev = _prev;
            else
            {
                _next._prev = _prev;
                _next = null;
            }
        }

        _prev = null;
        assert _next == null;
        
        return head;
    }

//    boolean isNormal ( Cur that )
//    {
//        return isNormal() && (that == null || (_locale == that._locale && that.isNormal()));
//    }
    
    boolean isNormal ( )
    {
        if (_state == POOLED || _state == DISPOSED)
            return false;
        
        if (_xobj == null)
            return _pos == NO_POS;

        if (!_xobj.isNormal( _pos ))
            return false;

        if (_state == EMBEDDED)
            return isOnList( _xobj._embedded );

        assert _state == REGISTERED;

        return isOnList( _locale._registered );
    }

    static final class CurLoadContext extends LoadContext
    {
        CurLoadContext ( Locale l, XmlOptions options )
        {
// TODO - use a thread local charUtil to load the xml -- don't use the
// locales charUtil, let the Locales _charUtil be specific to it because
// it is not thread safe to share a thread local charUtil between Locales
            
            options = options = XmlOptions.maskNull( options );

            if (options.hasOption( XmlOptions.LOAD_REPLACE_DOCUMENT_ELEMENT ))
            {
                _replaceDocElem = (QName) options.get( XmlOptions.LOAD_REPLACE_DOCUMENT_ELEMENT );
                _discardDocElem = true;
            }

            _stripWhitespace = options.hasOption( XmlOptions.LOAD_STRIP_WHITESPACE );
            _stripComments   = options.hasOption( XmlOptions.LOAD_STRIP_COMMENTS   );
            _stripProcinsts  = options.hasOption( XmlOptions.LOAD_STRIP_PROCINSTS  );
            
            _substituteNamespaces = (Map) options.get( XmlOptions.LOAD_SUBSTITUTE_NAMESPACES );
            _additionalNamespaces = (Map) options.get( XmlOptions.LOAD_ADDITIONAL_NAMESPACES );

            _locale = l;
            _frontier = createDomDocumentRootXobj( l );
            _after = false;
            
            _locale._versionAll++;
            _locale._versionSansText++;
        }

        private void start ( Xobj xo )
        {
            assert _frontier != null;
            assert !_after || _frontier._parent != null;

            if (_stripWhitespace)
                stripLeadingWhitespace();
            
            if (_after)
            {
                _frontier = _frontier._parent;
                _after = false;
            }

            _frontier.appendXobj( xo );
            _frontier = xo;
        }
        
        private void end ( )
        {
            assert _frontier != null;
            assert !_after || _frontier._parent != null;

            if (_stripWhitespace)
                stripLeadingWhitespace();
            
            if (_after)
                _frontier = _frontier._parent;
            else
                _after = true;
        }

        private QName checkName ( QName name, boolean local )
        {
            if (_substituteNamespaces != null && (!local || name.getNamespaceURI().length() > 0))
            {
                String substituteUri = (String) _substituteNamespaces.get( name.getNamespaceURI() );

                if (substituteUri != null)
                    name = _locale.makeQName( substituteUri, name.getLocalPart(), name.getPrefix());
            }

            return name;
        }
        
        protected void startElement ( QName name )
        {
            start(
                createElementXobj(
                    _locale, checkName( name, false ),
                    (_after ? _frontier._parent :_frontier)._name ) );
        }
        
        protected void endElement ( )
        {
            assert (_after ? _frontier._parent : _frontier).isElem();
            end();
        }
        
        protected void xmlns ( String prefix, String uri )
        {
            assert prefix == null || prefix.length() > 0;
            assert (_after ? _frontier._parent : _frontier).isContainer();

            start( new Xobj.AttrXobj( _locale, checkName( _locale.createXmlns( prefix ), true ) ) );

            text( uri, 0, uri.length() );
            
            end();
        }
        
        protected void attr ( String local, String uri, String prefix, String value )
        {
            assert (_after ? _frontier._parent : _frontier).isContainer();
            
            start(
                new Xobj.AttrXobj(
                    _locale, checkName( _locale.makeQName( uri, local, prefix ), true ) ) );
            
            text( value, 0, value.length() );
            end();
        }
        
        protected void procInst ( String target, String value )
        {
            if (!_stripProcinsts)
            {
                start( new Xobj.ProcInstXobj( _locale, target ) );
                text( value, 0, value.length() );
                end();
            }
        }
        
        protected void comment ( char[] buf, int off, int cch )
        {
            if (!_stripComments)
            {
                start( new Xobj.CommentXobj( _locale ) );
                text( (Object) buf, off, cch );
                end();
            }
        }

        private void stripLeadingWhitespace ( )
        {
            CharUtil cu = _locale._charUtil;
            
            if (_after)
            {
                _frontier._srcAfter =
                    cu.stripRight( _frontier._srcAfter, _frontier._offAfter, _frontier._cchAfter );
                
                _frontier._offAfter = cu._offSrc;
                _frontier._cchAfter = cu._cchSrc;
            }
            else
            {
                _frontier._srcValue =
                    cu.stripRight( _frontier._srcValue, _frontier._offValue, _frontier._cchValue );
                
                _frontier._offValue = cu._offSrc;
                _frontier._cchValue = cu._cchSrc;
            }
        }
        
        private void text ( Object src, int off, int cch )
        {
            if (cch <= 0)
                return;

            CharUtil cu = _locale._charUtil;

            if (_after)
            {
                _frontier._srcAfter =
                    cu.saveChars(
                        src, off, cch,
                        _frontier._srcAfter, _frontier._offAfter, _frontier._cchAfter );

                _frontier._offAfter = cu._offSrc;
                _frontier._cchAfter = cu._cchSrc;
            }
            else
            {
                _frontier._srcValue =
                    cu.saveChars(
                        src, off, cch,
                        _frontier._srcValue, _frontier._offValue, _frontier._cchValue );

                _frontier._offValue = cu._offSrc;
                _frontier._cchValue = cu._cchSrc;
            }
        }
        
        protected void text ( char[] src, int off, int cch )
        {
            Object srcObj = src;
            
            if (_stripWhitespace)
            {
                srcObj = _locale._charUtil.stripLeft( srcObj, off, cch );
                off = _locale._charUtil._offSrc;
                cch = _locale._charUtil._cchSrc;
            }
            
            text( srcObj, off, cch );
        }
        
        protected Cur finish ( )
        {
            if (_stripWhitespace)
                stripLeadingWhitespace();
            
            if (_after)
                _frontier = _frontier._parent;

            assert _frontier != null && _frontier._parent == null && _frontier.isRoot();

            Cur c = _frontier.tempCur();
            Locale.toFirstChildElement( c );

            // See if the document element is a fragment

            boolean isFrag =
                c.getName().equals( Locale._openuriFragment ) ||
                    c.getName().equals( Locale._xmlFragment );

            if (_discardDocElem || isFrag)
            {
                if (_replaceDocElem != null)
                    c.setName( _replaceDocElem );
                else
                {
                    // Remove the content around the element to remove so that that content
                    // does not appear to have been the contents of the removed element.

                    while ( c.toParent() )
                        ;
                    
                    c.next();

                    while ( !c.isElem() )
                        if (c.isText()) c.moveChars( null, -1 ); else c.moveNode( null );

                    assert c.isElem();
                    c.skip();

                    while ( !c.isFinish() )
                        if (c.isText()) c.moveChars( null, -1 ); else c.moveNode( null );

                    c.toParent();

                    c.next();

                    assert c.isElem();
                    
                    c.moveNodeContents( c, true );
                    
                    c.moveNode( null );
                }
                
                // Remove the fragment namespace decl
                
                if (isFrag)
                {
                    c.moveTo( _frontier );
                    
                    if (c.toFirstAttr())
                    {
                        for ( ; ; )
                        {
                            if (c.isXmlns() && c.getXmlnsUri().equals( Locale._openFragUri ))
                            {
                                c.moveNode( null );

                                if (!c.isAttr())
                                    break;
                            }
                            else if (!c.toNextAttr())
                                break;
                        }
                    }
                }
            }
            

            if (_additionalNamespaces != null)
            {
                c.moveTo( _frontier );
                
                Locale.toFirstChildElement( c );

                java.util.Iterator i = _additionalNamespaces.keySet().iterator();
                
                while ( i.hasNext() )
                {
                    String prefix = (String) i.next();

                    // Usually, this is the predefined xml namespace
                    if (!prefix.toLowerCase().startsWith( "xml" ))
                    {
                        if (c.namespaceForPrefix( prefix, false ) == null)
                        {
                            c.push();
                            
                            c.next();
                            c.createAttr( _locale.createXmlns( prefix ) );
                            c.next();
                            
                            String namespace = (String) _additionalNamespaces.get( prefix );
                            c.insertString( namespace );
                                          
                            c.pop();
                        }
                    }
                }
            }
            
            c.moveTo( _frontier );

            assert c.isRoot();
            
            return c;
        }

        public void dump ( )
        {
            _frontier.dump();
        }
        
        private Locale  _locale;
        private Xobj    _frontier;
        private boolean _after;
        private boolean _discardDocElem;
        private QName   _replaceDocElem;
        private boolean _stripWhitespace;
        private boolean _stripComments;
        private boolean _stripProcinsts;
        private Map     _substituteNamespaces;
        private Map     _additionalNamespaces;
    }

    //
    //
    //

    static String kindName ( int kind )
    {
        switch ( kind )
        {
            case ROOT     : return "ROOT";
            case ELEM     : return "ELEM";
            case ATTR     : return "ATTR";
            case COMMENT  : return "COMMENT";
            case PROCINST : return "PROCINST";
            case TEXT     : return "TEXT";
            default       : return "<< Unknown Kind (" + kind + ") >>";
        }
    }

    static void dump ( PrintStream o, Dom d, Object ref )
    {
    }
    
    static void dump ( PrintStream o, Dom d )
    {
        d.dump( o );
    }

    static void dump ( Dom d )
    {
        dump( System.out, d );
    }

    static void dump ( Node n )
    {
        dump( System.out, n );
    }

    static void dump ( PrintStream o, Node n )
    {
        dump( o, (Dom) n );
    }

    void dump ( )
    {
        dump( System.out, _xobj, this );
    }

    void dump ( PrintStream o )
    {
        if (_xobj == null)
        {
            o.println( "Unpositioned xptr" );
            return;
        }

        dump( o, _xobj, this );
    }

    public static void dump ( PrintStream o, Xobj xo, Object ref )
    {
        if (ref == null)
            ref = xo;
        
        while ( xo._parent != null )
            xo = xo._parent;

        dumpXobj( o, xo, 0, ref );
        
        o.println();
    }
    
    private static void dumpCur ( PrintStream o, String prefix, Cur c, Object ref )
    {
        o.print( " " );
        
        if (ref == c)
            o.print( "*:" );
        
        o.print( prefix + (c._id == null ? "<cur>" : c._id) + "[" + c._pos + "]" );
    }
    
    private static void dumpCurs ( PrintStream o, Xobj xo, Object ref )
    {
        for ( Cur c = xo._embedded ; c != null ; c = c._next )
            dumpCur( o, "E:", c, ref );
        
        for ( Cur c = xo._locale._registered ; c != null ; c = c._next )
        {
            if (c._xobj == xo)
                dumpCur( o, "R:", c, ref );
        }
    }
    
    private static void dumpBookmarks ( PrintStream o, Xobj xo, Object ref )
    {
        for ( Bookmark b = xo._bookmarks ; b != null ; b = b._next )
        {
            o.print( " " );
            
            if (ref == b)
                o.print( "*:" );
            
            o.print( "<mark>" + "[" + b._pos + "]" );
        }
    }
    
    private static void dumpCharNodes ( PrintStream o, CharNode nodes, Object ref )
    {
        for ( CharNode n = nodes ; n != null ; n = n._next )
        {
            o.print( " " );
            
            if (n == ref)
                o.print( "*" );
            
            o.print( (n instanceof TextNode ? "TEXT" : "CDATA") + "[" + n._cch + "]" );
        }
    }
    
    private static void dumpXobj ( PrintStream o, Xobj xo, int level, Object ref )
    {
        if (xo == null)
            return;

        if (xo == ref)
            o.print( "* " );
        else
            o.print( "  " );
        
        for ( int i = 0 ; i < level ; i++ )
            o.print( "  " );

        o.print( kindName( xo.kind() ) );

        if (xo._name != null)
        {
            o.print( " " );
            
            if (xo._name.getPrefix().length() > 0)
                o.print( xo._name.getPrefix() + ":" );
            
            o.print( xo._name.getLocalPart() );

            if (xo._name.getNamespaceURI().length() > 0)
                o.print( "@" + xo._name.getNamespaceURI() );
        }

        if (xo._srcValue != null || xo._charNodesValue != null)
        {
            o.print( " Value( " );
            o.print( "\"" + CharUtil.getString( xo._srcValue, xo._offValue, xo._cchValue ) + "\"" );
//            CharUtil.dumpChars( o, xo._srcValue, xo._offValue, xo._cchValue );
            dumpCharNodes( o, xo._charNodesValue, ref );
            o.print( " )" );
        }

        if (xo.isVacant())
            o.print( " (VACANT)" );

        if (xo._srcAfter != null || xo._charNodesAfter != null)
        {
            o.print( " After( " );
            o.print( "\"" + CharUtil.getString( xo._srcAfter, xo._offAfter, xo._cchAfter ) + "\"" );
//            CharUtil.dumpChars( o, xo._srcAfter, xo._offAfter, xo._cchAfter );
            dumpCharNodes( o, xo._charNodesAfter, ref );
            o.print( " )" );
        }

        dumpCurs( o, xo, ref );
        dumpBookmarks( o, xo, ref );

        String className = xo.getClass().getName();
        
        int i = className.lastIndexOf( '.' );
        
        if (i > 0)
        {
            className = className.substring( i + 1 );
            
            i = className.lastIndexOf( '$' );

            if (i > 0)
                className = className.substring( i + 1 );
        }

        o.print( " (" );
        o.print( className );
        o.print( ")" );
        
        o.println();

        for ( xo = xo._firstChild ; xo != null ; xo = xo._nextSibling )
            dumpXobj( o, xo, level + 1, ref );
    }

    void setId ( String id )
    {
        _id = id;
    }
    
    //
    //
    //

    Locale _locale;
    
    Xobj _xobj;
    int _pos;

    int _state;

    String _id;

    Cur _nextTemp;
    int _tempFrame;

    Cur _next;
    Cur _prev;
    
    Object _key;
    Object _value;

    int _stackTop;

    int _selectionFirst;
    int _selectionN;
    int _selectionLoc;
    int _selectionCount;
    
    private int _posTemp;
    
    int _offSrc;
    int _cchSrc;
}  