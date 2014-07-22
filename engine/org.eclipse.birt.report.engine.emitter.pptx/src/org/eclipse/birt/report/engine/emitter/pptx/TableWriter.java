
package org.eclipse.birt.report.engine.emitter.pptx;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;

import org.eclipse.birt.report.engine.content.ICellContent;
import org.eclipse.birt.report.engine.emitter.pptx.util.PPTXUtil;
import org.eclipse.birt.report.engine.nLayout.area.IArea;
import org.eclipse.birt.report.engine.nLayout.area.IContainerArea;
import org.eclipse.birt.report.engine.nLayout.area.impl.BlockTextArea;
import org.eclipse.birt.report.engine.nLayout.area.impl.CellArea;
import org.eclipse.birt.report.engine.nLayout.area.impl.ContainerArea;
import org.eclipse.birt.report.engine.nLayout.area.impl.RowArea;
import org.eclipse.birt.report.engine.nLayout.area.impl.TableArea;
import org.eclipse.birt.report.engine.nLayout.area.impl.TableGroupArea;
import org.eclipse.birt.report.engine.nLayout.area.style.BackgroundImageInfo;
import org.eclipse.birt.report.engine.nLayout.area.style.BorderInfo;
import org.eclipse.birt.report.engine.nLayout.area.style.BoxStyle;
import org.eclipse.birt.report.engine.nLayout.area.style.DiagonalInfo;
import org.eclipse.birt.report.engine.ooxml.writer.OOXmlWriter;

public class TableWriter
{

	private static final String RIGHTBORDERLINE = "a:lnR";
	private static final String LEFTBORDERLINE = "a:lnL";
	private static final String TOPBORDERLINE = "a:lnT";
	private static final String BOTTOMBORDERLINE = "a:lnB";
	private static final int DEFAULT_EMPTYCELL_FONTSIZE = 100;
	private int currentX;
	private int currentY;
	protected Stack<BoxStyle> rowStyleStack = new Stack<BoxStyle>( );
	private final float scale;
	private final PPTXRender render;
	private final PPTXPage graphics;
	private final PPTXCanvas canvas;
	protected OOXmlWriter writer;
	private static int TableIndex = 1;
	private int numOfColumns;
	private HashMap<Integer, MergeCellDimension> rowSpanCounts;
	private int currentCol;
	private int currentRow;
	private int colspan;
	private int rowspan;
	private TextWriter emptytextboxwriter;

	public TableWriter( PPTXRender render )
	{
		this.render = render;
		this.graphics = render.getGraphic( );
		this.canvas = render.getCanvas( );
		this.writer = canvas.getWriter( );
		scale = render.getScale( );
		currentX = render.getCurrentX( );
		currentY = render.getCurrentY( );
	}

	public void outputTable( TableArea table )
	{
		drawTable( table );
	}

	protected void drawTable( TableArea table )
	{
		if ( table.needClip( ) )
		{
			render.startClip( table );
		}

		currentX += getX( table );
		currentY += getY( table );
		updateRenderXY( );	
		parseTableExtraSpanRows( table );
		startTable( table );
		iterateOnRows( table );

		if ( table.needClip( ) )
		{
			// end clip...
		}

		if ( table.needClip( ) )
		{
			graphics.endClip( );
		}
		currentX -= getX( table );
		currentY -= getY( table );
		updateRenderXY( );		
		endTable( );
	}

	private void iterateOnRows( IContainerArea table )
	{
		int internalRowCount = currentRow;
		currentRow = 0;
		Iterator<IArea> iter = table.getChildren( );
		while ( iter.hasNext( ) )
		{
			IContainerArea child = (IContainerArea) iter.next( );
			if ( child instanceof RowArea )
			{
				drawRow( (RowArea) child );
				currentRow++;
			}
			else
			{// TableGroupArea:
				currentX += getX(  child );
				currentY += getY( child );
				internalRowCount = currentRow;
				updateRenderXY( );
				iterateOnRows( child );
				currentX -= getX( child );
				currentY -= getY( child );
				updateRenderXY( );
				currentRow = internalRowCount + 1;
			}
		}

	}
	
	/**
	 * precond: TableGroupArea do not have outside merging in
	 * @param table
	 */
	private void parseTableExtraSpanRows( ContainerArea table )
	{
		int additionalrowheight = 0;
		int additionalrowspan = 0;
		for ( int rowidx = table.getChildrenCount( ) - 1; rowidx >= 0; rowidx-- )
		{
			ContainerArea child = (ContainerArea) table
					.getChild( rowidx );
			if ( child instanceof TableGroupArea )
			{
				parseTableExtraSpanRows( child );
			}
			else
			{
				RowArea row = (RowArea) child;
				if ( row.getChildrenCount( ) == 0 )
				{
					additionalrowheight += row.getHeight( );
					additionalrowspan++;
				}
				else if ( additionalrowspan > 0 )
				{
					row.setHeight( additionalrowheight + row.getHeight( ) );
					Iterator<IArea> iter = row.getChildren( );
					while ( iter.hasNext( ) )
					{
						CellArea cell = (CellArea) iter.next( );
						cell.setRowSpan( cell.getRowSpan( ) - additionalrowspan );
					}
					additionalrowheight = 0;
					additionalrowspan = 0;
				}
			}
		}
	}

	private void startTable( TableArea tablearea )
	{
		int X = PPTXUtil.convertToEnums( currentX );
		int Y = PPTXUtil.convertToEnums( currentY );
		int width = PPTXUtil.convertToEnums( tablearea.getWidth( ) );
		int height = PPTXUtil.convertToEnums( tablearea.getHeight( ) );
		writer.openTag( "p:graphicFrame" );
		writer.openTag( "p:nvGraphicFramePr" );
		writer.openTag( "p:cNvPr" );
		writer.attribute( "id", canvas.getPresentation( ).getNextShapeId( ) );
		writer.attribute( "name", "Table " + TableIndex++ );
		writer.closeTag( "p:cNvPr" );
		writer.openTag( "p:cNvGraphicFramePr" );
		writer.openTag( "a:graphicFrameLocks" );
		writer.attribute( "noGrp", "1" );
		writer.closeTag( "a:graphicFrameLocks" );
		writer.closeTag( "p:cNvGraphicFramePr" );
		writer.openTag( "p:nvPr" );
		writer.closeTag( "p:nvPr" );
		writer.closeTag( "p:nvGraphicFramePr" );
		canvas.setPosition( 'p', X, Y, width, height );
		writer.openTag( "a:graphic" );
		writer.openTag( "a:graphicData" );
		writer.attribute( "uri",
				"http://schemas.openxmlformats.org/drawingml/2006/table" );
		writer.openTag( "a:tbl" );

		writer.openTag( "a:tblPr" );
		writer.openTag( "a:tableStyleId" );
		// use transparent table style:
		canvas.writeText( "{2D5ABB26-0587-4C30-8999-92F81FD0307C}" );
		writer.closeTag( "a:tableStyleId" );
		writer.closeTag( "a:tblPr" );
		writeColumnsWidth( tablearea );
	}

	private void endTable( )
	{
		writer.closeTag( "a:tbl" );
		writer.closeTag( "a:graphicData" );
		writer.closeTag( "a:graphic" );
		writer.closeTag( "p:graphicFrame" );
	}

	private void writeColumnsWidth( TableArea tablearea )
	{
		numOfColumns = tablearea.getColumnCount( );
		int columnWidth = 0;
		int cellwidth = 0;
		writer.openTag( "a:tblGrid" );
		int defaultwidth = tablearea.getWidth( ) / numOfColumns;
		for ( int i = 0; i < numOfColumns; i++ )
		{
			cellwidth = tablearea.getCellWidth( i, i + 1 );
			if ( cellwidth <= 0 )
			{
				cellwidth = defaultwidth;
			}

			columnWidth = PPTXUtil.convertToEnums( getScaledValue( cellwidth ) );

			writer.openTag( "a:gridCol" );
			writer.attribute( "w", columnWidth );
			writer.closeTag( "a:gridCol" );
		}
		writer.closeTag( "a:tblGrid" );
	}

	protected void drawRow( RowArea row )
	{
		row.setRowID( currentRow );
		if ( row.getChildrenCount( ) == 0 )
		{
			return;
		}
		currentX += getX( row );
		currentY += getY( row );
		updateRenderXY();
		BoxStyle style = row.getBoxStyle( );
		if ( style.getBackgroundColor( ) == null && style.getBackgroundImage( ) == null )
		{
			style = row.getParent( ).getBoxStyle( );
		}
		rowStyleStack.push( style );
		startRow( row ); // tags
		Iterator<IArea> iter = row.getChildren( );
		currentCol = 0;

		fillEmptyMergeCells( 0, 0, 0 );
		while ( iter.hasNext( ) )
		{
			IArea child = iter.next( );
			drawCell( (CellArea) child );
		}
		endRow( );

		rowStyleStack.pop( );
		currentX -= getX( row );
		currentY -= getY( row );
		updateRenderXY();		
	}

	private void startRow( RowArea row )
	{
		writer.openTag( "a:tr" );
		writer.attribute( "h", PPTXUtil.convertToEnums( row.getHeight( ) ) );

	}

	private void endRow( )
	{
		writer.closeTag( "a:tr" );
	}

	protected void drawCell( CellArea cell )
	{
		currentX += getX( cell );
		currentY += getY( cell );
		updateRenderXY( );
		startCell( cell );
		if ( cell.getChildrenCount( ) == 0 )
		{// draw emtpy textbox for size of cell to remain, use font size 1
			if ( emptytextboxwriter == null )
			{
				emptytextboxwriter = new TextWriter( render );
			}
			emptytextboxwriter.writeBlankTextBlock( DEFAULT_EMPTYCELL_FONTSIZE );
		}
		else
		{
			visitChildren( cell );
		}
		endCell( cell );
		currentX -= getX( cell );
		currentY -= getY( cell );
		updateRenderXY( );
	}

	/**
	 * start cell tag with all properties: styling
	 * 
	 * @param cell
	 */
	private void startCell( CellArea cell )
	{
		writer.openTag( "a:tc" );

		colspan = cell.getColSpan( );
		if ( colspan > 1 )
		{
			writer.attribute( "gridSpan", colspan );
		}
		rowspan = cell.getRowSpan( );
		if ( rowspan > 1 )
		{
			int colid = cell.getColumnID( );
			if ( rowSpanCounts == null )
			{
				rowSpanCounts = new HashMap<Integer, MergeCellDimension>( );
			}
			MergeCellDimension spancells = new MergeCellDimension( rowspan,
					colspan );
			rowSpanCounts.put( colid, spancells );
			writer.attribute( "rowSpan", rowspan );
		}
	}

	private void endCell( CellArea cell )
	{
		writer.openTag( "a:tcPr" );
		// it is set zero since no way to retrieve margin except to set public
		// CELL_DEFAULT
		canvas.writeMarginProperties( 0, 0, 0, 0 );

		ICellContent content = (ICellContent) cell.getContent( );
		String valign = content.getComputedStyle( ).getVerticalAlign( );
		if ( !( valign.equals( "baseline" ) || valign.equals( "top" ) ) )
		{
			if ( valign.equals( "middle" ) )
			{
				writer.attribute( "anchor", "ctr" );
			}
			else if ( valign.equals( "bottom" ) )
			{
				writer.attribute( "anchor", "b" );
			}
		}
		drawCellBox( cell );
		writer.closeTag( "a:tcPr" );
		writer.closeTag( "a:tc" );

		int nxtCol = currentCol + 1;
		fillEmptyMergeCells( nxtCol, colspan, rowspan );
		currentCol++;
	}

	private void fillEmptyMergeCells( int nxtCol, int icolspan, int irowspan )
	{
		boolean completedFill = false;
		boolean rectMerge = false;
		if ( rowSpanCounts != null && !rowSpanCounts.isEmpty( )
				&& rowSpanCounts.containsKey( nxtCol ) )
		{
			completedFill = true;
			MergeCellDimension mcd = rowSpanCounts.get( nxtCol );
			icolspan = mcd.getNumColumns( );
			writer.openTag( "a:tc" );
			if ( icolspan > 1 )
			{
				writer.attribute( "gridSpan", icolspan );
				rectMerge = true;
			}
			writer.attribute( "vMerge", 1 );
			writer.openTag( "a:tcPr" );
			canvas.writeMarginProperties( 0, 0, 0, 0 );
			writer.closeTag( "a:tcPr" );
			writer.closeTag( "a:tc" );
			mcd.removeARow( );
			if ( mcd.isLastRow( ) )
			{
				rowSpanCounts.remove( nxtCol );
			}
			currentCol++;
		}

		if ( icolspan > 1 )
		{
			for ( int emtpycell = 1; emtpycell < icolspan; emtpycell++ )
			{
				writer.openTag( "a:tc" );
				writer.attribute( "hMerge", 1 );
				if ( irowspan > 1 )
				{
					writer.attribute( "rowSpan", irowspan );
				}
				else if ( rectMerge )
				{
					writer.attribute( "vMerge", 1 );
				}
				writer.openTag( "a:tcPr" );
				canvas.writeMarginProperties( 0, 0, 0, 0 );
				writer.closeTag( "a:tcPr" );
				writer.closeTag( "a:tc" );
			}
			completedFill = true;
			currentCol = currentCol + icolspan - 1;
		}
		if ( completedFill )
		{
			nxtCol += icolspan;
			fillEmptyMergeCells( nxtCol, 0, 0 );
		}
	}

	protected void visitChildren( IContainerArea container )
	{
		Iterator<IArea> iter = container.getChildren( );
		while ( iter.hasNext( ) )
		{
			IArea child = iter.next( );
			if ( child instanceof BlockTextArea )
			{
				if ( container.getChildrenCount( ) > 1 )
				{
					render.visitTextBuffer( (BlockTextArea) child );
				}
				else
				{
					child.accept( render );
				}

			}
			else
			{
				child.accept( render );
			}
		}
	}

	/**
	 * draw the cells properties: only one fill is allow, background image goes
	 * over background color
	 * 
	 * @param cell
	 */
	protected void drawCellBox( CellArea cell )
	{
		drawBorders( cell );
		drawCellDiagonal( cell );

		BoxStyle style = cell.getBoxStyle( );
		Color backgroundcolor = style.getBackgroundColor( );
		BackgroundImageInfo bgimginfo = style.getBackgroundImage( );

		if ( !rowStyleStack.isEmpty( )
				&& ( backgroundcolor == null || bgimginfo == null ) )
		{
			BoxStyle rowStyle = rowStyleStack.peek( );
			if ( rowStyle != null )
			{
				if ( backgroundcolor == null )
				{
					backgroundcolor = rowStyle.getBackgroundColor( );
				}
				if ( bgimginfo == null )
				{
					bgimginfo = rowStyle.getBackgroundImage( );
				}
			}
		}

		// String imageUrl = EmitterUtil.getBackgroundImageUrl(
		// style,reportDesign );

		if ( bgimginfo != null )
		{
			float offsetY = 0;
			float offsetX = 0;
			int repeatmode = bgimginfo.getRepeatedMode( );

			if ( repeatmode == BackgroundImageInfo.NO_REPEAT )
			{
				int imgheight = PPTXUtil.pixelToEmu( (int) bgimginfo
						.getImageInstance( ).getHeight( ), bgimginfo
						.getImageInstance( ).getDpiY( ) );
				int imgwidth = PPTXUtil.pixelToEmu( (int) bgimginfo
						.getImageInstance( ).getWidth( ), bgimginfo
						.getImageInstance( ).getDpiX( ) );
				int cellheight = PPTXUtil.convertToEnums( getScaledValue( cell
						.getHeight( ) ) );
				int cellwidth = PPTXUtil.convertToEnums( getScaledValue( cell
						.getWidth( ) ) );
				offsetY = PPTXUtil
						.parsePercentageOffset( cellheight, imgheight );
				offsetX = PPTXUtil.parsePercentageOffset( cellwidth, imgwidth );
			}
			canvas.setBackgroundImg( canvas.getImageRelationship( bgimginfo ),
					(int) offsetX, (int) offsetY, repeatmode );
		}
		else if ( backgroundcolor != null )
		{
			canvas.setBackgroundColor( backgroundcolor );
		}

	}

	protected void drawCellDiagonal( CellArea cell )
	{
		DiagonalInfo diagonalInfo = cell.getDiagonalInfo( );
		if ( diagonalInfo != null && diagonalInfo.getDiagonalNumber( ) == 1 )
		{// only support single line : width should be the same as borders

			writer.openTag( "a:lnTlToBr" );
			int width = PPTXUtil.convertToEnums( diagonalInfo
					.getDiagonalWidth( ) );
			writer.attribute( "w", width );
			writer.attribute( "cap", "flat" );
			writer.attribute( "algn", "ctr" );
			canvas.setBackgroundColor( diagonalInfo.getDiagonalColor( ) );
			writer.openTag( "a:prstDash" );
			writer.attribute( "val",
					PPTXUtil.parseStyle( diagonalInfo.getDiagonalStyle( ) ) );
			writer.closeTag( "a:prstDash" );
			writer.openTag( "a:round" );
			writer.closeTag( "a:round" );
			writer.openTag( "a:headEnd" );
			writer.attribute( "type", "none" );
			writer.attribute( "w", "med" );
			writer.attribute( "len", "med" );
			writer.closeTag( "a:headEnd" );
			writer.openTag( "a:tailEnd" );
			writer.attribute( "type", "none" );
			writer.attribute( "w", "med" );
			writer.attribute( "len", "med" );
			writer.closeTag( "a:tailEnd" );
			writer.closeTag( "a:lnTlToBr" );
		}
	}

	/**
	 * assume leftborder is always draw
	 * 
	 * @param container
	 */
	protected void drawBorders( CellArea container )
	{
		BoxStyle style = container.getBoxStyle( );
		if ( style == null )
			return;

		BorderInfo currentborderinfo = style.getLeftBorder( );

		writeSingleBorder( LEFTBORDERLINE, currentborderinfo );

		currentborderinfo = style.getRightBorder( );
		if ( currentborderinfo != null )
		{
			writeSingleBorder( RIGHTBORDERLINE, currentborderinfo );
		}
		else
		{ // draw if border is empty:
			CellArea nextcell = ( (RowArea) container.getParent( ) )
					.getCell( currentCol + colspan );
			if ( nextcell != null )
			{
				writeSingleBorder( RIGHTBORDERLINE, nextcell.getBoxStyle( )
						.getLeftBorder( ) );
			}
		}

		writeSingleBorder( TOPBORDERLINE, style.getTopBorder( ) );

		//check below cell first for bottomline
		currentborderinfo = null;
		RowArea currentRowArea = (RowArea) container.getParent( );
		ContainerArea grandparent = currentRowArea.getParent( );
		
		IArea nextcontainer = grandparent.getChild( currentRowArea.getRowID( ) + 1 );
		while ( nextcontainer == null && grandparent instanceof TableGroupArea )
		{// end of table grouparea
			IArea currentTableGroup = grandparent;
			grandparent = grandparent.getParent( );
			// get next child after currenttablegroup
			Iterator<IArea> rowiter = grandparent.getChildren( );
			while ( rowiter.hasNext( )
					&& !( rowiter.next( ).equals( currentTableGroup ) ) );
			if ( rowiter.hasNext( ) )
			{
				nextcontainer = rowiter.next( );
			}
		}
		RowArea ra = null;
		while ( nextcontainer instanceof TableGroupArea )
		{
			nextcontainer = ( (TableGroupArea) nextcontainer ).getFirstChild( );
		}
		ra = (RowArea) nextcontainer;

		CellArea belowCell = null;
		if ( ra != null )
		{
			belowCell = ra.getCell( currentCol );
			if ( belowCell != null )
			{
				currentborderinfo = belowCell.getBoxStyle( ).getTopBorder( );
				writeSingleBorder( BOTTOMBORDERLINE, currentborderinfo );
			}
		}
		if ( currentborderinfo == null )
		{
			writeSingleBorder( BOTTOMBORDERLINE, style.getBottomBorder( ) );
		}
	}

	private void writeSingleBorder( String borderSide, BorderInfo borderinfo )
	{
		if ( borderinfo == null )
		{
			return;
		}
		writer.openTag( borderSide );
		int width = PPTXUtil.convertToEnums( borderinfo.getWidth( ) );
		writer.attribute( "w", width );
		canvas.setBackgroundColor( borderinfo.getColor( ) );
		writer.openTag( "a:prstDash" );
		writer.attribute( "val", PPTXUtil.parseStyle( borderinfo.getStyle( ) ) );
		writer.closeTag( "a:prstDash" );
		writer.closeTag( borderSide );
	}

	private int getY( IContainerArea area )
	{
		return getScaledValue( area.getY( ) );
	}

	private int getX( IContainerArea area )
	{
		return getScaledValue( area.getX( ) );
	}

	protected int getScaledValue( int value )
	{
		return (int) ( value * scale );
	}

	protected void updateRenderXY( )
	{
		render.setCurrentX( currentX );
		render.setCurrentY( currentY );
	}

	private class MergeCellDimension
	{

		private int rows;
		private final int columns;

		public MergeCellDimension( int rows, int columns )
		{
			this.rows = rows;
			this.columns = columns;
		}

		public int getNumRows( )
		{
			return rows;
		}

		public int getNumColumns( )
		{
			return columns;
		}

		public void removeARow( )
		{
			rows--;
		}

		public boolean isLastRow( )
		{
			return ( rows == 1 );
		}
	}

}
